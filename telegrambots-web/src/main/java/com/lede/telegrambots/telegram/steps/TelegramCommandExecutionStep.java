package com.lede.telegrambots.telegram.steps;

import com.lede.telegrambots.telegram.TelegramWebhookStep;
import com.lede.telegrambots.telegram.impl.TelegramWebhookContext;
import com.lede.telegrambots.telegram.impl.TelegramWebhookResult;

import com.lede.telegrambots.telegram.TelegramWebhookStep;

import com.lede.telegrambots.telegram.*;

import com.lede.telegrambots.telegram.command.CommandContext;
import com.lede.telegrambots.application.port.out.TelegramGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(6)
public class TelegramCommandExecutionStep implements TelegramWebhookStep {

    private static final Logger log = LoggerFactory.getLogger(TelegramCommandExecutionStep.class);
    private final TelegramGateway sender;

    public TelegramCommandExecutionStep(TelegramGateway sender) {
        this.sender = sender;
    }

    @Override
    public Optional<TelegramWebhookResult> execute(TelegramWebhookContext context) {
        try {
            Optional<String> reply = context.getCommand().execute(
                    new CommandContext(context.getChatId(), context.getCommandArg(), context.getBot())
            );
            reply.ifPresent(html -> sender.sendHtml(context.getBot().token(), context.getChatId(), html));
            return Optional.of(TelegramWebhookResult.of(TelegramWebhookResult.Outcome.OK, "ok"));
        } catch (Exception e) {
            log.error("Command {} on @{} threw", context.getCommandKey(), context.getBot().username(), e);
            return Optional.of(TelegramWebhookResult.of(TelegramWebhookResult.Outcome.EXECUTION_ERROR, "execution error"));
        }
    }
}
