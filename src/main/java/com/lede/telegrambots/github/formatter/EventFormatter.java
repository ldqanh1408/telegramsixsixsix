package com.lede.telegrambots.github.formatter;

import tools.jackson.databind.JsonNode;

import java.util.Optional;

/**
 * Formats a single GitHub webhook event type into a Telegram HTML message.
 *
 * <p>Implementations are Spring beans and self-register by declaring
 * the event name they handle (matches GitHub's {@code X-GitHub-Event} header).
 * Return {@link Optional#empty()} when the payload should be silently dropped
 * (e.g. an action variant we don't care about).</p>
 */
public interface EventFormatter {

    /** GitHub event name, e.g. {@code "push"}, {@code "pull_request"}. */
    String eventName();

    Optional<String> format(JsonNode payload);
}
