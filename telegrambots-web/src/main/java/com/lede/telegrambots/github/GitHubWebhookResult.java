package com.lede.telegrambots.github;

/**
 * Web-agnostic outcome of processing a GitHub webhook delivery.
 *
 * <p>The {@link GitHubWebhookProcessor} returns this so the application layer never
 * touches Spring MVC types; {@code GitHubWebhookController} alone maps {@link Outcome}
 * to an HTTP status. This keeps the processing rules decoupled from the web framework.</p>
 */
public record GitHubWebhookResult(Outcome outcome, String body) {

    public enum Outcome {
        UNKNOWN_BOT,
        BAD_SIGNATURE,
        MISSING_EVENT,
        PONG,
        INVALID_JSON,
        REPO_MISMATCH,
        OK
    }

    public static GitHubWebhookResult of(Outcome outcome, String body) {
        return new GitHubWebhookResult(outcome, body);
    }
}
