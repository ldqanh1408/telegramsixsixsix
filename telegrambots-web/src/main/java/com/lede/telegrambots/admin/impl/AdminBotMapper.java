package com.lede.telegrambots.admin.impl;

import com.lede.telegrambots.admin.dto.AdminBotRequest;
import com.lede.telegrambots.admin.dto.AdminBotResponse;
import com.lede.telegrambots.application.port.in.BotManagementUseCase;
import com.lede.telegrambots.domain.bot.BotRegistration;
import com.lede.telegrambots.domain.bot.ManagedBot;
import org.springframework.stereotype.Component;

@Component
class AdminBotMapper {

    private final BotManagementUseCase bots;

    AdminBotMapper(BotManagementUseCase bots) {
        this.bots = bots;
    }

    AdminBotResponse toResponse(ManagedBot bot) {
        return new AdminBotResponse(
                bot.id(),
                bot.username(),
                bot.enabled(),
                bot.githubRepo(),
                hasText(bot.token()),
                hasText(bot.tgWebhookSecret()),
                hasText(bot.ghWebhookSecret()),
                "/telegram/webhook/" + bot.username(),
                "/github/webhook/" + bot.username(),
                bots.activeGroupCount(bot),
                bot.createdAt(),
                bot.updatedAt()
        );
    }

    BotRegistration toRegistration(String usernameOverride, AdminBotRequest request) {
        String effectiveUsername = usernameOverride == null ? request.username() : usernameOverride;
        return new BotRegistration(
                effectiveUsername,
                request.token(),
                request.telegramWebhookSecret(),
                request.githubRepo(),
                request.githubWebhookSecret(),
                request.enabled()
        );
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
