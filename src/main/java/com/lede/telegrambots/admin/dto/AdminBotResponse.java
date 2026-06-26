package com.lede.telegrambots.admin.dto;

import java.time.Instant;

public record AdminBotResponse(
        String id,
        String username,
        boolean enabled,
        String githubRepo,
        boolean hasToken,
        boolean hasTelegramWebhookSecret,
        boolean hasGithubWebhookSecret,
        String telegramWebhookPath,
        String githubWebhookPath,
        long activeGroups,
        Instant createdAt,
        Instant updatedAt
) {
}
