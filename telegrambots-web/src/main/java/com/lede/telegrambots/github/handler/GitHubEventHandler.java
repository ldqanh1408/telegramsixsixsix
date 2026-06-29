package com.lede.telegrambots.github.handler;

import com.lede.telegrambots.mongo.entity.ManagedBot;
import tools.jackson.databind.JsonNode;

/**
 * Strategy interface for handling GitHub webhook events.
 */
public interface GitHubEventHandler {

    /**
     * Checks if this handler supports the given GitHub event.
     *
     * @param event the GitHub event name (from X-GitHub-Event header)
     * @return true if supported, false otherwise
     */
    boolean supports(String event);

    /**
     * Executes the business logic for the GitHub event.
     *
     * @param bot the bot context
     * @param payload the JSON payload of the event
     */
    void execute(ManagedBot bot, JsonNode payload);
}
