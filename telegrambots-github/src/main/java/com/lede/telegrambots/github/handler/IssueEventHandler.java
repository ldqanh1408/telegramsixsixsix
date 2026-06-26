package com.lede.telegrambots.github.handler;

import com.lede.telegrambots.github.formatter.EventFormatter;
import com.lede.telegrambots.mongo.entity.ManagedBot;
import com.lede.telegrambots.notification.NotificationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
class IssueEventHandler implements GitHubEventHandler {

    private final EventFormatter formatter;
    private final NotificationService notifications;

    public IssueEventHandler(@Qualifier("issueEventFormatter") EventFormatter formatter, NotificationService notifications) {
        this.formatter = formatter;
        this.notifications = notifications;
    }

    @Override
    public boolean supports(String event) {
        return "issues".equals(event);
    }

    @Override
    public void execute(ManagedBot bot, JsonNode payload) {
        formatter.format(payload).ifPresent(html -> notifications.broadcast(bot, html));
    }
}
