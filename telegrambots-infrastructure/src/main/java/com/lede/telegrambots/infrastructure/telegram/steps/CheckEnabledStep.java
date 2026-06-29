package com.lede.telegrambots.infrastructure.telegram.steps;

import com.lede.telegrambots.application.pipeline.Step;
import com.lede.telegrambots.infrastructure.telegram.SyncSavedBotContext;
import com.lede.telegrambots.infrastructure.telegram.TelegramClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;

public record CheckEnabledStep(TelegramClient telegramClient) implements Step<SyncSavedBotContext, Boolean> {
    private static final Logger log = LoggerFactory.getLogger(CheckEnabledStep.class);

    @Override
    public Optional<Boolean> execute(SyncSavedBotContext ctx) {
        if (!ctx.getEvent().enabled()) {
            log.info("Bot @{} is disabled, deleting Telegram webhook", ctx.getEvent().username());
            telegramClient.deleteWebhook(ctx.getEvent().token());
            return Optional.of(Boolean.FALSE);
        }
        return Optional.empty();
    }
}
