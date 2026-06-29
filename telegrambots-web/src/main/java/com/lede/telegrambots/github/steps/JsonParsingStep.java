package com.lede.telegrambots.github.steps;

import com.lede.telegrambots.github.GitHubWebhookContext;
import com.lede.telegrambots.github.GitHubWebhookResult;
import com.lede.telegrambots.github.GitHubWebhookStep;

import com.lede.telegrambots.github.GitHubWebhookResult.Outcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@Component
@Order(5)
public class JsonParsingStep implements GitHubWebhookStep {

    private static final Logger log = LoggerFactory.getLogger(JsonParsingStep.class);
    private final ObjectMapper mapper;

    public JsonParsingStep(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<GitHubWebhookResult> execute(GitHubWebhookContext context) {
        try {
            JsonNode payload = mapper.readTree(context.getBody());
            context.setPayload(payload);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Invalid JSON in webhook delivery={} for @{}",
                    context.getDelivery(), context.getBot().username(), e);
            return Optional.of(GitHubWebhookResult.of(Outcome.INVALID_JSON, "invalid json"));
        }
    }
}
