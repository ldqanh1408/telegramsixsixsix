package com.lede.telegrambots.github.steps;

import com.lede.telegrambots.github.impl.GitHubWebhookContext;
import com.lede.telegrambots.github.GitHubWebhookResult;
import com.lede.telegrambots.github.GitHubWebhookStep;

import com.lede.telegrambots.application.port.out.WebhookSignatureVerifier;
import com.lede.telegrambots.github.GitHubWebhookResult.Outcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(2)
public class SignatureVerificationStep implements GitHubWebhookStep {

    private static final Logger log = LoggerFactory.getLogger(SignatureVerificationStep.class);
    private final WebhookSignatureVerifier signatureVerifier;

    public SignatureVerificationStep(WebhookSignatureVerifier signatureVerifier) {
        this.signatureVerifier = signatureVerifier;
    }

    @Override
    public Optional<GitHubWebhookResult> execute(GitHubWebhookContext context) {
        if (!signatureVerifier.verify(context.getBot().ghWebhookSecret(), context.getSignature(), context.getBody())) {
            log.warn("Rejected GitHub webhook delivery={} for @{}: bad signature",
                    context.getDelivery(), context.getBot().username());
            return Optional.of(GitHubWebhookResult.of(Outcome.BAD_SIGNATURE, "bad signature"));
        }
        return Optional.empty();
    }
}
