package com.lede.telegrambots.domain.bot;

/**
 * Domain event published when a managed bot is registered or updated.
 */
public record BotSavedEvent(
        String username,
        String token,
        String tgWebhookSecret,
        boolean enabled
) {
}
