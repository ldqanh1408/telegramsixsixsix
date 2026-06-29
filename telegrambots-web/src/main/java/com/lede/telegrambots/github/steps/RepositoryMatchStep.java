package com.lede.telegrambots.github.steps;

import com.lede.telegrambots.github.impl.GitHubWebhookContext;
import com.lede.telegrambots.github.GitHubWebhookResult;
import com.lede.telegrambots.github.GitHubWebhookStep;

import com.lede.telegrambots.github.GitHubWebhookResult.Outcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(6)
public class RepositoryMatchStep implements GitHubWebhookStep {

    private static final Logger log = LoggerFactory.getLogger(RepositoryMatchStep.class);

    @Override
    public Optional<GitHubWebhookResult> execute(GitHubWebhookContext context) {
        if (!matchesConfiguredRepo(context)) {
            log.debug("Ignored event={} for repo={} on @{} (configured={})",
                    context.getEvent(),
                    context.getPayload().path("repository").path("full_name").asText(""),
                    context.getBot().username(),
                    context.getBot().githubRepo());
            return Optional.of(GitHubWebhookResult.of(Outcome.REPO_MISMATCH, "ignored: repo mismatch"));
        }
        return Optional.empty();
    }

    private boolean matchesConfiguredRepo(GitHubWebhookContext context) {
        String configuredRepo = context.getBot().githubRepo();
        if (configuredRepo == null || configuredRepo.isBlank()) return true;
        String fullName = context.getPayload().path("repository").path("full_name").asText("");
        return configuredRepo.equalsIgnoreCase(fullName);
    }
}
