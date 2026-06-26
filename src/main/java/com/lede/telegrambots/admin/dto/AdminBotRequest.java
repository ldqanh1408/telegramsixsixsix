package com.lede.telegrambots.admin.dto;

import com.lede.telegrambots.bot.BotRegistration;

public record AdminBotRequest(
        String username,
        String token,
        String telegramWebhookSecret,
        String githubRepo,
        String githubWebhookSecret,
        Boolean enabled
) {
    public BotRegistration toRegistration(String usernameOverride) {
        String effectiveUsername = usernameOverride == null ? username : usernameOverride;
        return new BotRegistration(
                effectiveUsername,
                token,
                telegramWebhookSecret,
                githubRepo,
                githubWebhookSecret,
                enabled
        );
    }
}
