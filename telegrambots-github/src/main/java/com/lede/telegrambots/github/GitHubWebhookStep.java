package com.lede.telegrambots.github;

import java.util.Optional;

/**
 * Functional interface for a single step in the GitHub webhook processing pipeline.
 */
@FunctionalInterface
interface GitHubWebhookStep {

    /**
     * Executes the step's logic.
     *
     * @param context the context holding input parameters and intermediate/shared data
     * @return Optional containing the result if the step wishes to short-circuit the pipeline,
     *         or Optional.empty() if it wishes to proceed to the next step
     */
    Optional<GitHubWebhookResult> execute(GitHubWebhookContext context);
}
