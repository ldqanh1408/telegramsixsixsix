package com.lede.telegrambots.bot;

/**
 * Event published when a managed bot is deleted.
 */
public record BotDeletedEvent(
        String username,
        String token
) {
}
