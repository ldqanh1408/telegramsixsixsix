package com.lede.telegrambots.admin.impl;



import com.lede.telegrambots.admin.dto.AdminBotRequest;
import com.lede.telegrambots.admin.dto.AdminBotResponse;
import com.lede.telegrambots.application.port.in.BotManagementUseCase;
import com.lede.telegrambots.domain.bot.ManagedBot;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
class AdminBotService {

    private final BotManagementUseCase bots;
    private final AdminBotMapper mapper;

    AdminBotService(BotManagementUseCase bots, AdminBotMapper mapper) {
        this.bots = bots;
        this.mapper = mapper;
    }

    List<AdminBotResponse> list() {
        return bots.listBots().stream()
                .map(mapper::toResponse)
                .toList();
    }

    Optional<AdminBotResponse> find(String username) {
        return bots.findAny(username)
                .map(mapper::toResponse);
    }

    AdminBotResponse save(String usernameOverride, AdminBotRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        ManagedBot saved = bots.upsertBot(request.toRegistration(usernameOverride));
        return mapper.toResponse(saved);
    }

    void delete(String username) {
        bots.deleteBot(username);
    }
}
