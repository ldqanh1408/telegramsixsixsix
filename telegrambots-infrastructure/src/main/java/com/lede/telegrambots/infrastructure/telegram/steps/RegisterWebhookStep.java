package com.lede.telegrambots.infrastructure.telegram.steps;

import com.lede.telegrambots.application.pipeline.Step;
import com.lede.telegrambots.infrastructure.telegram.SyncSavedBotContext;
import com.lede.telegrambots.infrastructure.telegram.TelegramClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;

public record RegisterWebhookStep(TelegramClient telegramClient) implements Step<SyncSavedBotContext, Boolean> {
    private static final Logger log = LoggerFactory.getLogger(RegisterWebhookStep.class);

    @Override
    public Optional<Boolean> execute(SyncSavedBotContext ctx) {
        log.info("Bot @{} is enabled, setting Telegram webhook to: {}", ctx.getEvent().username(), ctx.getWebhookUrl());
        boolean success = telegramClient.setWebhook(
                ctx.getEvent().token(),
                ctx.getWebhookUrl(),
                ctx.getEvent().tgWebhookSecret()
        );
        ctx.setSuccess(success);
        if (success) {
            log.info("Successfully registered Telegram webhook for @{}", ctx.getEvent().username());
        } else {
            log.error("Failed to register Telegram webhook for @{}", ctx.getEvent().username());
        }
        return Optional.of(success);
    }
}
