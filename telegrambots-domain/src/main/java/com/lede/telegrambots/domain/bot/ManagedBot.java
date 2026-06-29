package com.lede.telegrambots.domain.bot;

import java.time.Instant;
import java.util.UUID;

public class ManagedBot {
    private String id;
    private String username;
    private String token;
    private String tgWebhookSecret;
    private String githubRepo;
    private String ghWebhookSecret;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;

    public ManagedBot(
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
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        this.id = id;
        this.username = username;
        this.token = token;
        this.tgWebhookSecret = tgWebhookSecret;
        this.githubRepo = githubRepo;
        this.ghWebhookSecret = ghWebhookSecret;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String id() { return id; }
    public String username() { return username; }
    public String token() { return token; }
    public String tgWebhookSecret() { return tgWebhookSecret; }
    public String githubRepo() { return githubRepo; }
    public String ghWebhookSecret() { return ghWebhookSecret; }
    public boolean enabled() { return enabled; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    public void merge(BotRegistration registration) {
        requireValid(this.username, registration, this.token);

        this.token = getOrExisting(registration.tokenOrNull(), this.token);
        this.enabled = getOrExisting(registration.enabled(), this.enabled);
        this.githubRepo = getOrExisting(registration.githubRepoOrNull(), this.githubRepo);
        this.tgWebhookSecret = resolveSecret(registration.telegramWebhookSecretOrNull(), this.tgWebhookSecret);
        this.ghWebhookSecret = resolveSecret(registration.githubWebhookSecretOrNull(), this.ghWebhookSecret);

        this.updatedAt = Instant.now();
    }

    public static ManagedBot create(String username, BotRegistration registration) {
        requireValid(username, registration, null);

        Instant now = Instant.now();
        boolean enabled = registration.enabled() != null ? registration.enabled() : true;

        return new ManagedBot(
                null,
                username,
                registration.tokenOrNull(),
                resolveSecret(registration.telegramWebhookSecretOrNull(), null),
                registration.githubRepoOrNull(),
                resolveSecret(registration.githubWebhookSecretOrNull(), null),
                enabled,
                now,
                now
        );
    }

    private static <T> T getOrExisting(T incoming, T existing) {
        return incoming != null ? incoming : existing;
    }

    private static String resolveSecret(String incoming, String existing) {
        if (incoming != null && !incoming.isBlank()) return incoming;
        if (existing != null && !existing.isBlank()) return existing;
        return generateSecret();
    }

    private static void requireValid(String username, BotRegistration registration, String existingToken) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        if (registration.tokenOrNull() == null && existingToken == null) {
            throw new IllegalArgumentException("token is required for a new bot");
        }
    }

    private static String generateSecret() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
