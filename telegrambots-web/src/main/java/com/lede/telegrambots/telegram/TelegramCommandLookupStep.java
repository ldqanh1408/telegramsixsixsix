package com.lede.telegrambots.telegram;

import com.lede.telegrambots.telegram.command.BotCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import java.util.Optional;

@Component
@Order(5)
class TelegramCommandLookupStep implements TelegramWebhookStep {

    private static final Logger log = LoggerFactory.getLogger(TelegramCommandLookupStep.class);
    private final Map<String, BotCommand> commands;

    public TelegramCommandLookupStep(List<BotCommand> commands) {
        this.commands = commands.stream()
                .collect(Collectors.toUnmodifiableMap(BotCommand::name, Function.identity()));
        log.info("Registered {} bot command(s) in lookup step: {}", this.commands.size(), this.commands.keySet());
    }

    @Override
    public Optional<TelegramWebhookResult> execute(TelegramWebhookContext context) {
        BotCommand cmd = commands.get(context.getCommandKey());
        if (cmd == null) {
            log.debug("Unknown command on @{}: {}", context.getBot().username(), context.getCommandKey());
            return Optional.of(TelegramWebhookResult.of(TelegramWebhookResult.Outcome.UNKNOWN_COMMAND, "unknown command"));
        }
        context.setCommand(cmd);
        return Optional.empty();
    }
}
