package com.lede.telegrambots.notification;

import com.lede.telegrambots.bot.BotManagementUseCase;
import com.lede.telegrambots.github.formatter.EventFormatter;
import com.lede.telegrambots.mongo.entity.GroupActivation;
import com.lede.telegrambots.mongo.entity.ManagedBot;
import com.lede.telegrambots.telegram.TelegramClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Receives parsed GitHub events, renders them, and fans the result out to the
 * active Telegram groups for the managed bot that owns the webhook URL.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final Map<String, EventFormatter> formatters;
    private final TelegramClient telegram;
    private final BotManagementUseCase bots;

    public NotificationService(List<EventFormatter> formatters,
                               TelegramClient telegram,
                               BotManagementUseCase bots) {
        this.formatters = formatters.stream()
                .collect(Collectors.toUnmodifiableMap(EventFormatter::eventName, Function.identity()));
        this.telegram = telegram;
        this.bots = bots;
        log.info("Registered {} event formatter(s): {}", this.formatters.size(), this.formatters.keySet());
    }

    public void dispatch(ManagedBot bot, String event, JsonNode payload) {
        EventFormatter formatter = formatters.get(event);
        if (formatter == null) {
            log.debug("No formatter for event={} (ignored)", event);
            return;
        }
        Optional<String> message = formatter.format(payload);
        if (message.isEmpty()) {
            log.debug("Formatter for {} chose to drop this payload", event);
            return;
        }
        broadcast(bot, message.get(), event);
    }

    private void broadcast(ManagedBot bot, String html, String event) {
        List<GroupActivation> targets = bots.activeGroups(bot);
        if (targets.isEmpty()) {
            log.info("Event {} formatted for @{} but no activated groups - dropped", event, bot.username());
            return;
        }
        for (GroupActivation activation : targets) {
            telegram.sendHtml(bot.token(), activation.chatId(), html);
        }
    }
}
