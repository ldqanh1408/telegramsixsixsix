package com.lede.telegrambots.admin;

import com.lede.telegrambots.admin.dto.AdminBotResponse;
import com.lede.telegrambots.bot.BotManagementUseCase;
import com.lede.telegrambots.mongo.entity.ManagedBot;
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

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
