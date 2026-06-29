package com.lede.telegrambots.infrastructure.telegram.steps;

import com.lede.telegrambots.application.pipeline.Step;
import com.lede.telegrambots.infrastructure.telegram.SyncDeletedBotContext;
import com.lede.telegrambots.infrastructure.telegram.TelegramClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;

public record DeleteWebhookOnDeleteStep(TelegramClient telegramClient) implements Step<SyncDeletedBotContext, Boolean> {
    private static final Logger log = LoggerFactory.getLogger(DeleteWebhookOnDeleteStep.class);

    @Override
    public Optional<Boolean> execute(SyncDeletedBotContext ctx) {
        log.info("Bot @{} was deleted, deleting Telegram webhook", ctx.getEvent().username());
        boolean success = telegramClient.deleteWebhook(ctx.getEvent().token());
        return Optional.of(success);
    }
}
