package com.lede.telegrambots.github.steps;

import com.lede.telegrambots.github.GitHubWebhookContext;
import com.lede.telegrambots.github.GitHubWebhookResult;
import com.lede.telegrambots.github.GitHubWebhookStep;

import com.lede.telegrambots.github.GitHubWebhookResult.Outcome;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(3)
public class EventPresenceStep implements GitHubWebhookStep {

    @Override
    public Optional<GitHubWebhookResult> execute(GitHubWebhookContext context) {
        if (context.getEvent() == null) {
            return Optional.of(GitHubWebhookResult.of(Outcome.MISSING_EVENT, "missing X-GitHub-Event"));
        }
        return Optional.empty();
    }
}
