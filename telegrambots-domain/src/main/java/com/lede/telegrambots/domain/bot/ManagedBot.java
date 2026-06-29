package com.lede.telegrambots.domain.bot;

import java.time.Instant;

/**
 * One bot managed by this backend — a pure enterprise entity, free of any persistence or
 * framework annotations. The Mongo mapping lives in the infrastructure layer
 * ({@code ManagedBotDocument}) and is translated to/from this record by a mapper.
 *
 * <p>The {@code username} is the URL key — webhooks arrive at
 * {@code /telegram/webhook/{username}} and {@code /github/webhook/{username}}.</p>
 *
 * <p>Each bot owns its own Telegram token, GitHub repo, and webhook secrets.
 * Multiple bots can coexist in the same backend process.</p>
 */
public record ManagedBot(
        String id,
        String username,
        String token,
        String tgWebhookSecret,
        String githubRepo,
        String ghWebhookSecret,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
