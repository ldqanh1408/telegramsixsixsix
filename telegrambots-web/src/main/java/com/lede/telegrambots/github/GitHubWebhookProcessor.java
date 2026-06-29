package com.lede.telegrambots.github;

import com.lede.telegrambots.github.GitHubWebhookResult.Outcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Application service that orchestrates the GitHub webhook processing pipeline
 * using the Component-based Pipeline Pattern (Spring Beans + @Order).
 */
@Service
class GitHubWebhookProcessor {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookProcessor.class);

    private final List<GitHubWebhookStep> steps;

    GitHubWebhookProcessor(List<GitHubWebhookStep> steps) {
        this.steps = steps;
    }

    GitHubWebhookResult process(String botUsername, String event, String signature,
                                String delivery, byte[] body) {

        GitHubWebhookContext context = new GitHubWebhookContext(
                botUsername, event, signature, delivery, body
        );

        for (GitHubWebhookStep step : steps) {
            Optional<GitHubWebhookResult> shortCircuit = step.execute(context);
            if (shortCircuit.isPresent()) {
                return shortCircuit.get();
            }
        }

        // Default fallback success (normally short-circuited by EventExecutionStep)
        return GitHubWebhookResult.of(Outcome.OK, "ok");
    }
}
