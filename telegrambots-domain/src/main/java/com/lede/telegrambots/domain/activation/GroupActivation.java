package com.lede.telegrambots.domain.activation;

import java.time.Instant;

/**
 * One activation = a binding "this bot is allowed to post in this Telegram chat" — a pure
 * enterprise entity. The Mongo mapping (collection + indexes) lives in the infrastructure layer
 * ({@code GroupActivationDocument}).
 *
 * <p>Created when an admin in the chat issues {@code /add @<botUsername>}.</p>
 */
public record GroupActivation(
        String id,
        String botId,
        String botUsername,
        long chatId,
        boolean active,
        Instant activatedAt,
        Instant updatedAt
) {
}
