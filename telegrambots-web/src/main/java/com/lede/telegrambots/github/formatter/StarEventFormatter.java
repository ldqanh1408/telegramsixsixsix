package com.lede.telegrambots.github.formatter;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.Optional;

import static com.lede.telegrambots.shared.MessageFormatter.code;
import static com.lede.telegrambots.shared.MessageFormatter.esc;

@Component
class StarEventFormatter implements EventFormatter {

    @Override public String eventName() { return "star"; }

    @Override
    public Optional<String> format(JsonNode p) {
        if (!"created".equals(p.path("action").asText(""))) return Optional.empty();

        String user = p.path("sender").path("login").asText("?");
        String repo = p.path("repository").path("full_name").asText("");
        int stars = p.path("repository").path("stargazers_count").asInt();

        return Optional.of("⭐ " + esc(user) + " starred " + code(repo) + " (total: " + stars + ")");
    }
}
