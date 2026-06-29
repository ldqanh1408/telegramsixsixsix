package com.lede.telegrambots.domain.bot;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain service holding the pure business rules for {@link ManagedBot}: validation, merging an
 * incoming update onto the existing state, and automatic secret generation. No framework coupling.
 */
public class BotDomainService {

    public ManagedBot prepareForSave(ManagedBot current, String username, BotRegistration registration) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }

        if (registration.tokenOrNull() == null && current == null) {
            throw new IllegalArgumentException("token is required for a new bot");
        }

        Instant now = Instant.now();
        boolean enabled = registration.enabled() == null || registration.enabled();
        if (current != null && registration.enabled() == null) {
            enabled = current.enabled();
        }

        String incomingTgSecret = registration.telegramWebhookSecretOrNull();
        String existingTgSecret = current == null ? null : current.tgWebhookSecret();
        String finalTgSecret = valueOrExisting(incomingTgSecret, existingTgSecret);
        if (finalTgSecret == null || finalTgSecret.isBlank()) {
            finalTgSecret = generateSecret();
        }

        String incomingGhSecret = registration.githubWebhookSecretOrNull();
        String existingGhSecret = current == null ? null : current.ghWebhookSecret();
        String finalGhSecret = valueOrExisting(incomingGhSecret, existingGhSecret);
        if (finalGhSecret == null || finalGhSecret.isBlank()) {
            finalGhSecret = generateSecret();
        }

        return new ManagedBot(
                current == null ? null : current.id(),
                username,
                valueOrExisting(registration.tokenOrNull(), current == null ? null : current.token()),
                finalTgSecret,
                valueOrExisting(registration.githubRepoOrNull(), current == null ? null : current.githubRepo()),
                finalGhSecret,
                enabled,
                current == null ? now : current.createdAt(),
                now
        );
    }

    private String generateSecret() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String valueOrExisting(String incoming, String existing) {
        if (incoming == null) return existing;
        return incoming;
    }
}
