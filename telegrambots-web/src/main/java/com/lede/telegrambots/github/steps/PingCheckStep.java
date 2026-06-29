package com.lede.telegrambots.github.steps;

import com.lede.telegrambots.github.GitHubWebhookContext;
import com.lede.telegrambots.github.GitHubWebhookResult;
import com.lede.telegrambots.github.GitHubWebhookStep;

import com.lede.telegrambots.github.GitHubWebhookResult.Outcome;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(4)
public class PingCheckStep implements GitHubWebhookStep {

    @Override
    public Optional<GitHubWebhookResult> execute(GitHubWebhookContext context) {
        if ("ping".equals(context.getEvent())) {
            return Optional.of(GitHubWebhookResult.of(Outcome.PONG, "pong"));
        }
        return Optional.empty();
    }
}
