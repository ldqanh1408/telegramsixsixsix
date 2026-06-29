package com.lede.telegrambots.github.steps;

import com.lede.telegrambots.application.notification.BroadcastUseCase;
import com.lede.telegrambots.github.GitHubEventRenderer;
import com.lede.telegrambots.github.GitHubWebhookResult;
import com.lede.telegrambots.github.GitHubWebhookResult.Outcome;
import com.lede.telegrambots.github.GitHubWebhookStep;
import com.lede.telegrambots.github.impl.GitHubWebhookContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(7)
public class EventExecutionStep implements GitHubWebhookStep {

    private static final Logger log = LoggerFactory.getLogger(EventExecutionStep.class);
    private final GitHubEventRenderer renderer;
    private final BroadcastUseCase notifications;

    public EventExecutionStep(GitHubEventRenderer renderer, BroadcastUseCase notifications) {
        this.renderer = renderer;
        this.notifications = notifications;
    }

    @Override
    public Optional<GitHubWebhookResult> execute(GitHubWebhookContext context) {
        try {
            renderer.render(context.getEvent(), context.getPayload())
                    .ifPresent(html -> notifications.broadcast(context.getBot(), html));
        } catch (Exception e) {
            log.error("Error broadcasting GitHub event={} delivery={} for @{}",
                    context.getEvent(), context.getDelivery(), context.getBot().username(), e);
        }

        return Optional.of(GitHubWebhookResult.of(Outcome.OK, "ok"));
    }
}
