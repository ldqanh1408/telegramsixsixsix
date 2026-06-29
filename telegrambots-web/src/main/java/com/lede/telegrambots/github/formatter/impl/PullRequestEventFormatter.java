package com.lede.telegrambots.github.formatter.impl;

import com.lede.telegrambots.github.formatter.*;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.Optional;

import static com.lede.telegrambots.domain.shared.MessageFormatter.bold;
import static com.lede.telegrambots.domain.shared.MessageFormatter.code;
import static com.lede.telegrambots.domain.shared.MessageFormatter.esc;
import static com.lede.telegrambots.domain.shared.MessageFormatter.link;

@Component
class PullRequestEventFormatter implements EventFormatter {

    @Override public String eventName() { return "pull_request"; }

    @Override
    public Optional<String> format(JsonNode p) {
        String action = p.path("action").asText("");
        JsonNode pr = p.path("pull_request");
        boolean merged = pr.path("merged").asBoolean(false);

        String verb = switch (action) {
            case "opened"           -> "🟢 PR mở";
            case "reopened"         -> "🟢 PR mở lại";
            case "closed"           -> merged ? "🟣 PR merged" : "🔴 PR closed";
            case "ready_for_review" -> "👀 PR sẵn sàng review";
            default -> null;
        };
        if (verb == null) return Optional.empty();

        String repo = p.path("repository").path("full_name").asText("");
        int number = pr.path("number").asInt();
        String title = pr.path("title").asText("");
        String url = pr.path("html_url").asText("");
        String user = pr.path("user").path("login").asText("?");

        return Optional.of(verb + " " + link("#" + number, url) + " trong " + code(repo)
                + "\n" + bold(title) + "\nbởi " + esc(user));
    }
}
