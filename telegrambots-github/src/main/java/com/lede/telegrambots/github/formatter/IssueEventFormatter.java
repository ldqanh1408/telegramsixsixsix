package com.lede.telegrambots.github.formatter;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.Optional;

import static com.lede.telegrambots.shared.MessageFormatter.bold;
import static com.lede.telegrambots.shared.MessageFormatter.code;
import static com.lede.telegrambots.shared.MessageFormatter.esc;
import static com.lede.telegrambots.shared.MessageFormatter.link;

@Component
class IssueEventFormatter implements EventFormatter {

    @Override public String eventName() { return "issues"; }

    @Override
    public Optional<String> format(JsonNode p) {
        String verb = switch (p.path("action").asText("")) {
            case "opened"   -> "🐛 Issue mới";
            case "reopened" -> "🐛 Issue reopened";
            case "closed"   -> "✅ Issue closed";
            default -> null;
        };
        if (verb == null) return Optional.empty();

        JsonNode issue = p.path("issue");
        String repo = p.path("repository").path("full_name").asText("");
        int number = issue.path("number").asInt();
        String title = issue.path("title").asText("");
        String url = issue.path("html_url").asText("");
        String user = issue.path("user").path("login").asText("?");

        return Optional.of(verb + " " + link("#" + number, url) + " trong " + code(repo)
                + "\n" + bold(title) + "\nbởi " + esc(user));
    }
}
