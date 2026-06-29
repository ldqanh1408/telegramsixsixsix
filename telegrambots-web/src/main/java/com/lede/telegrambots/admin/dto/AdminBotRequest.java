package com.lede.telegrambots.admin.dto;

public record AdminBotRequest(
        String username,
        String token,
        String telegramWebhookSecret,
        String githubRepo,
        String githubWebhookSecret,
        Boolean enabled
) {
}
