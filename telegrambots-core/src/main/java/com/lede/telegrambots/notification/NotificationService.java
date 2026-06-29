package com.lede.telegrambots.notification;

import com.lede.telegrambots.bot.BotManagementUseCase;
import com.lede.telegrambots.mongo.entity.GroupActivation;
import com.lede.telegrambots.mongo.entity.ManagedBot;
import com.lede.telegrambots.shared.TelegramSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Generic broadcaster: delivers an already-rendered HTML message to every active Telegram
 * group of a managed bot.
 *
 * <p>This module is source-agnostic — it knows nothing about GitHub. Event rendering lives
 * in the {@code github} module ({@code GitHubEventRenderer}); keeping it out of here removes
 * the former {@code github} ⇄ {@code notification} cycle, so the dependency flows one way:
 * {@code github → notification}.</p>
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final TelegramSender telegram;
    private final BotManagementUseCase bots;

    public NotificationService(TelegramSender telegram, BotManagementUseCase bots) {
        this.telegram = telegram;
        this.bots = bots;
    }

    /** Fan out one HTML message to all groups where this bot is active. */
    public void broadcast(ManagedBot bot, String html) {
        List<GroupActivation> targets = bots.activeGroups(bot);
        if (targets.isEmpty()) {
            log.info("Message for @{} but no activated groups - dropped", bot.username());
            return;
        }
        for (GroupActivation activation : targets) {
            telegram.sendHtml(bot.token(), activation.chatId(), html);
        }
    }
}
