package com.lede.telegrambots.github;

import com.lede.telegrambots.github.impl.GitHubWebhookContext;


import com.lede.telegrambots.github.*;

import com.lede.telegrambots.application.pipeline.Step;

/**
 * A single stage of the GitHub webhook pipeline. A typed alias over the shared
 * {@link Step} abstraction so the github steps form a distinct, generic-aware bean group
 * that Spring can inject as a {@code List<GitHubWebhookStep>}.
 */
public interface GitHubWebhookStep extends Step<GitHubWebhookContext, GitHubWebhookResult> {
}
