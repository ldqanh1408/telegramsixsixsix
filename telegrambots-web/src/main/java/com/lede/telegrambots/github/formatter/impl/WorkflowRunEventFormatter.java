package com.lede.telegrambots.github.formatter.impl;

import com.lede.telegrambots.github.formatter.EventFormatter;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.Optional;

import static com.lede.telegrambots.domain.shared.MessageFormatter.bold;
import static com.lede.telegrambots.domain.shared.MessageFormatter.esc;
import static com.lede.telegrambots.domain.shared.MessageFormatter.link;

@Component
class WorkflowRunEventFormatter implements EventFormatter {

    @Override public String eventName() { return "workflow_run"; }

    @Override
    public Optional<String> format(JsonNode p) {
        if (!"completed".equals(p.path("action").asText(""))) return Optional.empty();

        JsonNode run = p.path("workflow_run");
        String name = run.path("name").asText("workflow");
        String conclusion = run.path("conclusion").asText("");
        String url = run.path("html_url").asText("");
        String icon = switch (conclusion) {
            case "success"   -> "✅";
            case "failure"   -> "❌";
            case "cancelled" -> "⚪";
            default -> "ℹ️";
        };
        return Optional.of(icon + " " + bold(name) + " — " + esc(conclusion)
                + " " + link("logs", url));
    }
}
