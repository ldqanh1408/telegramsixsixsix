package com.lede.telegrambots.github;

import com.lede.telegrambots.github.formatter.EventFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Renders a GitHub webhook payload into a Telegram HTML message using the
 * {@link EventFormatter} strategy registered for that event type.
 *
 * <p>Owned by the {@code github} module because rendering GitHub events is GitHub-specific
 * knowledge. The {@code notification} module only broadcasts the resulting message and stays
 * source-agnostic.</p>
 */
@Component
class GitHubEventRenderer {

    private static final Logger log = LoggerFactory.getLogger(GitHubEventRenderer.class);

    private final Map<String, EventFormatter> formatters;

    GitHubEventRenderer(List<EventFormatter> formatters) {
        this.formatters = formatters.stream()
                .collect(Collectors.toUnmodifiableMap(EventFormatter::eventName, Function.identity()));
        log.info("Registered {} event formatter(s): {}", this.formatters.size(), this.formatters.keySet());
    }

    Optional<String> render(String event, JsonNode payload) {
        EventFormatter formatter = formatters.get(event);
        if (formatter == null) {
            log.debug("No formatter for event={} (ignored)", event);
            return Optional.empty();
        }
        Optional<String> message = formatter.format(payload);
        if (message.isEmpty()) {
            log.debug("Formatter for {} chose to drop this payload", event);
        }
        return message;
    }
}
