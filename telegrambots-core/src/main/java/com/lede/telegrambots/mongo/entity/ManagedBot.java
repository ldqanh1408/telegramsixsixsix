package com.lede.telegrambots.mongo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * One bot managed by this backend.
 *
 * <p>The {@code username} is the URL key — webhooks arrive at
 * {@code /telegram/webhook/{username}} and {@code /github/webhook/{username}}.</p>
 *
 * <p>Each bot owns its own Telegram token, GitHub repo, and webhook secrets.
 * Multiple bots can coexist in the same backend process.</p>
 */
@Document(collection = "managed_bots")
public record ManagedBot(
        @Id String id,
        @Indexed(unique = true) String username,
        String token,
        String tgWebhookSecret,
        String githubRepo,
        String ghWebhookSecret,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
