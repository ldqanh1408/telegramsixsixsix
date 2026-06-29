package com.lede.telegrambots.infrastructure.telegram;

import com.lede.telegrambots.domain.pipeline.Pipeline;
import com.lede.telegrambots.domain.pipeline.Step;
import com.lede.telegrambots.domain.bot.BotDeletedEvent;
import com.lede.telegrambots.domain.bot.BotSavedEvent;
import com.lede.telegrambots.infrastructure.telegram.steps.*;
import org.springframework.beans.factory.annotation.Autowired;
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

    // Standard constructor for test convenience
    public TelegramWebhookManager(TelegramClient telegramClient, String publicUrl) {
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

    // Spring wired constructor
    @Autowired
    public TelegramWebhookManager(
            List<Step<SyncSavedBotContext, Boolean>> savedSteps,
            List<Step<SyncDeletedBotContext, Boolean>> deletedSteps,
            @Value("${app.public-url:}") String publicUrl) {
        this.publicUrl = publicUrl != null ? publicUrl.trim() : "";
        this.savedPipeline = new Pipeline<>(savedSteps);
        this.deletedPipeline = new Pipeline<>(deletedSteps);
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
