package com.lede.telegrambots.github.impl;

import com.lede.telegrambots.github.GitHubWebhookStep;

import com.lede.telegrambots.github.*;

import com.lede.telegrambots.application.pipeline.Pipeline;
import com.lede.telegrambots.github.GitHubWebhookResult.Outcome;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates the GitHub webhook processing flow by running the ordered {@link GitHubWebhookStep}
 * beans through the shared {@link Pipeline}. The first step that short-circuits wins; otherwise the
 * default success is returned.
 */
@Service
class GitHubWebhookProcessor {

    private final Pipeline<GitHubWebhookContext, GitHubWebhookResult> pipeline;

    GitHubWebhookProcessor(List<GitHubWebhookStep> steps) {
        this.pipeline = new Pipeline<>(steps);
    }

    GitHubWebhookResult process(String botUsername, String event, String signature,
                                String delivery, byte[] body) {

        GitHubWebhookContext context = new GitHubWebhookContext(
                botUsername, event, signature, delivery, body
        );

        // Default fallback success (normally short-circuited by EventExecutionStep)
        return pipeline.run(context)
                .orElseGet(() -> GitHubWebhookResult.of(Outcome.OK, "ok"));
    }
}
