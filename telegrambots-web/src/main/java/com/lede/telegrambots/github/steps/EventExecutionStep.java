package com.lede.telegrambots.github.steps;

import com.lede.telegrambots.github.impl.GitHubWebhookContext;
import com.lede.telegrambots.github.GitHubWebhookResult;
import com.lede.telegrambots.github.GitHubWebhookStep;

import com.lede.telegrambots.github.GitHubWebhookResult.Outcome;
import com.lede.telegrambots.github.handler.GitHubEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Order(7)
public class EventExecutionStep implements GitHubWebhookStep {

    private static final Logger log = LoggerFactory.getLogger(EventExecutionStep.class);
    private final List<GitHubEventHandler> handlers;

    public EventExecutionStep(List<GitHubEventHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public Optional<GitHubWebhookResult> execute(GitHubWebhookContext context) {
        boolean handled = false;
        String event = context.getEvent();
        for (GitHubEventHandler handler : handlers) {
            if (handler.supports(event)) {
                try {
                    handler.execute(context.getBot(), context.getPayload());
                    handled = true;
                } catch (Exception e) {
                    log.error("Error executing handler for event={} delivery={} for @{}",
                            event, context.getDelivery(), context.getBot().username(), e);
                }
                break;
            }
        }

        if (!handled) {
            log.debug("No handler found/executed for event={}", event);
        }

        return Optional.of(GitHubWebhookResult.of(Outcome.OK, "ok"));
    }
}
