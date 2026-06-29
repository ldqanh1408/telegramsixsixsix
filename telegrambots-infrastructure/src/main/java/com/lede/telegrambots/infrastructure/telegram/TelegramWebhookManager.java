package com.lede.telegrambots.infrastructure.telegram;

import com.lede.telegrambots.application.pipeline.Pipeline;
import com.lede.telegrambots.domain.bot.BotDeletedEvent;
import com.lede.telegrambots.domain.bot.BotSavedEvent;
import com.lede.telegrambots.infrastructure.telegram.steps.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Event-driven adapter that keeps each bot's Telegram webhook registration in sync with its state.
 * Reacts to the domain events emitted by the bot use cases (delivered via Spring) and calls the
 * Telegram API through {@link TelegramClient}. Lives in infrastructure because it performs an
 * external side effect.
 */
@Component
public class TelegramWebhookManager {

    private final String publicUrl;
    private final Pipeline<SyncSavedBotContext, Boolean> savedPipeline;
    private final Pipeline<SyncDeletedBotContext, Boolean> deletedPipeline;

    public TelegramWebhookManager(
            TelegramClient telegramClient,
            @Value("${app.public-url:}") String publicUrl) {
        this.publicUrl = publicUrl != null ? publicUrl.trim() : "";
        this.savedPipeline = new Pipeline<>(List.of(
                new CheckEnabledStep(telegramClient),
                new CheckPublicUrlStep(),
                new FormatWebhookUrlStep(),
                new RegisterWebhookStep(telegramClient)
        ));
        this.deletedPipeline = new Pipeline<>(List.of(
                new DeleteWebhookOnDeleteStep(telegramClient)
        ));
    }

    @EventListener
    public void onBotSaved(BotSavedEvent event) {
        savedPipeline.run(new SyncSavedBotContext(event, publicUrl));
    }

    @EventListener
    public void onBotDeleted(BotDeletedEvent event) {
        deletedPipeline.run(new SyncDeletedBotContext(event));
    }
}
