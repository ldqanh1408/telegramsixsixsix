package com.lede.telegrambots.domain.bot;

/**
 * Domain event published when a managed bot is deleted.
 */
public record BotDeletedEvent(
        String username,
        String token
) {
}
