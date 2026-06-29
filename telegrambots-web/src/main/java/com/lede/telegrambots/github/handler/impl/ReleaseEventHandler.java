package com.lede.telegrambots.github.handler.impl;

import com.lede.telegrambots.github.handler.*;

import com.lede.telegrambots.github.formatter.EventFormatter;
import com.lede.telegrambots.domain.bot.ManagedBot;
import com.lede.telegrambots.application.notification.BroadcastUseCase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
class ReleaseEventHandler implements GitHubEventHandler {

    private final EventFormatter formatter;
    private final BroadcastUseCase notifications;

    public ReleaseEventHandler(@Qualifier("releaseEventFormatter") EventFormatter formatter, BroadcastUseCase notifications) {
        this.formatter = formatter;
        this.notifications = notifications;
    }

    @Override
    public boolean supports(String event) {
        return "release".equals(event);
    }

    @Override
    public void execute(ManagedBot bot, JsonNode payload) {
        formatter.format(payload).ifPresent(html -> notifications.broadcast(bot, html));
    }
}
