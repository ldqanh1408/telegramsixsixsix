package com.lede.telegrambots.bot;

/**
 * Event published when a managed bot is registered or updated.
 */
public record BotSavedEvent(
        String username,
        String token,
        String tgWebhookSecret,
        boolean enabled
) {
}
