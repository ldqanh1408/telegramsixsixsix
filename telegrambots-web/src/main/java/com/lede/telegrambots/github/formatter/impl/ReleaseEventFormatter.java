package com.lede.telegrambots.github.formatter.impl;

import com.lede.telegrambots.github.formatter.EventFormatter;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.Optional;

import static com.lede.telegrambots.domain.shared.MessageFormatter.bold;
import static com.lede.telegrambots.domain.shared.MessageFormatter.code;
import static com.lede.telegrambots.domain.shared.MessageFormatter.link;

@Component
class ReleaseEventFormatter implements EventFormatter {

    @Override public String eventName() { return "release"; }

    @Override
    public Optional<String> format(JsonNode p) {
        if (!"published".equals(p.path("action").asText(""))) return Optional.empty();

        JsonNode rel = p.path("release");
        String name = rel.path("name").asText("");
        if (name.isBlank()) name = rel.path("tag_name").asText("");
        String url = rel.path("html_url").asText("");
        String repo = p.path("repository").path("full_name").asText("");

        return Optional.of("🚀 Release " + bold(name) + " " + link("xem", url)
                + " — " + code(repo));
    }
}
