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
class PushEventFormatter implements EventFormatter {

    private static final int MAX_COMMITS_SHOWN = 5;
    private static final String BRANCH_PREFIX = "refs/heads/";

    @Override public String eventName() { return "push"; }

    @Override
    public Optional<String> format(JsonNode p) {
        JsonNode commits = p.path("commits");
        int count = commits.isArray() ? commits.size() : 0;
        if (count == 0) return Optional.empty();      // branch create/delete with no commits

        String repo = p.path("repository").path("full_name").asText("");
        String ref = p.path("ref").asText("");
        String branch = ref.startsWith(BRANCH_PREFIX) ? ref.substring(BRANCH_PREFIX.length()) : ref;
        String pusher = p.path("pusher").path("name").asText("?");
        String compareUrl = p.path("compare").asText("");

        StringBuilder sb = new StringBuilder()
                .append("🔔 ").append(bold(count + " commit" + (count == 1 ? "" : "s")))
                .append(" lên ").append(code(repo)).append(" @ ").append(code(branch))
                .append(" bởi ").append(esc(pusher)).append('\n');

        int shown = Math.min(count, MAX_COMMITS_SHOWN);
        for (int i = 0; i < shown; i++) {
            JsonNode c = commits.get(i);
            sb.append("• ")
                    .append(link(shortSha(c.path("id").asText("")), c.path("url").asText("")))
                    .append(' ').append(esc(firstLine(c.path("message").asText(""))))
                    .append(" — ").append(esc(c.path("author").path("name").asText("?")))
                    .append('\n');
        }
        if (count > shown && !compareUrl.isBlank()) {
            sb.append(link("…và " + (count - shown) + " commit khác", compareUrl));
        }
        return Optional.of(sb.toString());
    }

    private static String shortSha(String sha) {
        return sha.length() >= 7 ? sha.substring(0, 7) : sha;
    }

    private static String firstLine(String s) {
        int nl = s.indexOf('\n');
        return nl >= 0 ? s.substring(0, nl) : s;
    }
}
