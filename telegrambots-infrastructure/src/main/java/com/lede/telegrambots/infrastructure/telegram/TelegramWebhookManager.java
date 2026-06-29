package com.lede.telegrambots.infrastructure.telegram;

import com.lede.telegrambots.domain.bot.BotDeletedEvent;
import com.lede.telegrambots.domain.bot.BotSavedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event-driven adapter that keeps each bot's Telegram webhook registration in sync with its state.
 * Reacts to the domain events emitted by the bot use cases (delivered via Spring) and calls the
 * Telegram API through {@link TelegramClient}. Lives in infrastructure because it performs an
 * external side effect.
 */
@Component
public class TelegramWebhookManager {

    private static final Logger log = LoggerFactory.getLogger(TelegramWebhookManager.class);

    private final TelegramClient telegramClient;
    private final String publicUrl;

    public TelegramWebhookManager(
            TelegramClient telegramClient,
            @Value("${app.public-url:}") String publicUrl) {
        this.telegramClient = telegramClient;
        this.publicUrl = publicUrl != null ? publicUrl.trim() : "";
    }

    @EventListener
    public void onBotSaved(BotSavedEvent event) {
        if (!event.enabled()) {
            log.info("Bot @{} is disabled, deleting Telegram webhook", event.username());
            telegramClient.deleteWebhook(event.token());
            return;
        }

        if (publicUrl.isEmpty()) {
            log.warn("PUBLIC_URL is not configured; automatic Telegram setWebhook for @{} is skipped. " +
                    "Please configure app.public-url in application.yaml or set PUBLIC_URL environment variable.", event.username());
            return;
        }

        String webhookUrl = publicUrl;
        if (!webhookUrl.endsWith("/")) {
            webhookUrl += "/";
        }
        webhookUrl += "telegram/webhook/" + event.username();

        log.info("Bot @{} is enabled, setting Telegram webhook to: {}", event.username(), webhookUrl);
        boolean success = telegramClient.setWebhook(event.token(), webhookUrl, event.tgWebhookSecret());
        if (success) {
            log.info("Successfully registered Telegram webhook for @{}", event.username());
        } else {
            log.error("Failed to register Telegram webhook for @{}", event.username());
        }
    }

    @EventListener
    public void onBotDeleted(BotDeletedEvent event) {
        log.info("Bot @{} was deleted, deleting Telegram webhook", event.username());
        telegramClient.deleteWebhook(event.token());
    }
}
