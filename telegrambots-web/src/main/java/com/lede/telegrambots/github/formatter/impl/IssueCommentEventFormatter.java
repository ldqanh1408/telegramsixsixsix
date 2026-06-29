package com.lede.telegrambots.github.formatter.impl;

import com.lede.telegrambots.github.formatter.*;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.Optional;

import static com.lede.telegrambots.domain.shared.MessageFormatter.bold;
import static com.lede.telegrambots.domain.shared.MessageFormatter.esc;
import static com.lede.telegrambots.domain.shared.MessageFormatter.link;

@Component
class IssueCommentEventFormatter implements EventFormatter {

    @Override public String eventName() { return "issue_comment"; }

    @Override
    public Optional<String> format(JsonNode p) {
        if (!"created".equals(p.path("action").asText(""))) return Optional.empty();

        JsonNode issue = p.path("issue");
        JsonNode comment = p.path("comment");
        int number = issue.path("number").asInt();
        String title = issue.path("title").asText("");
        String url = comment.path("html_url").asText("");
        String user = comment.path("user").path("login").asText("?");

        return Optional.of("💬 Bình luận mới trên " + link("#" + number, url)
                + "\n" + bold(title) + "\nbởi " + esc(user));
    }
}
