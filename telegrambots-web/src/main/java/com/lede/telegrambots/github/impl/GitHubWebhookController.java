package com.lede.telegrambots.github.impl;

import com.lede.telegrambots.github.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin HTTP adapter for GitHub webhooks. It extracts the request, delegates all rules to
 * {@link GitHubWebhookProcessor}, and maps the web-agnostic {@link GitHubWebhookResult}
 * back to an HTTP status. No business logic lives here.
 */
@RestController
@RequestMapping("/github/webhook")
public class GitHubWebhookController {

    private final GitHubWebhookProcessor processor;

    public GitHubWebhookController(GitHubWebhookProcessor processor) {
        this.processor = processor;
    }

    @PostMapping("/{botUsername}")
    public ResponseEntity<String> onEvent(
            @PathVariable String botUsername,
            @RequestHeader(value = "X-GitHub-Event", required = false) String event,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String delivery,
            @RequestBody byte[] body) {

        GitHubWebhookResult result = processor.process(botUsername, event, signature, delivery, body);
        return ResponseEntity.status(toStatus(result.outcome())).body(result.body());
    }

    private static HttpStatus toStatus(GitHubWebhookResult.Outcome outcome) {
        return switch (outcome) {
            case UNKNOWN_BOT -> HttpStatus.NOT_FOUND;
            case BAD_SIGNATURE -> HttpStatus.UNAUTHORIZED;
            case MISSING_EVENT, INVALID_JSON -> HttpStatus.BAD_REQUEST;
            case PONG, REPO_MISMATCH, OK -> HttpStatus.OK;
        };
    }
}
