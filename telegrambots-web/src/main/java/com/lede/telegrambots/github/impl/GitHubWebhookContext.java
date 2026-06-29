package com.lede.telegrambots.github.impl;

import com.lede.telegrambots.domain.bot.ManagedBot;
import tools.jackson.databind.JsonNode;

/**
 * Context object holding input parameters and intermediate/shared data throughout
 * the GitHub webhook processing pipeline.
 */
public class GitHubWebhookContext {
    private final String botUsername;
    private final String event;
    private final String signature;
    private final String delivery;
    private final byte[] body;

    // Shared data mutated by steps
    private ManagedBot bot;
    private JsonNode payload;

    public GitHubWebhookContext(String botUsername, String event, String signature, String delivery, byte[] body) {
        this.botUsername = botUsername;
        this.event = event;
        this.signature = signature;
        this.delivery = delivery;
        this.body = body;
    }

    public String getBotUsername() {
        return botUsername;
    }

    public String getEvent() {
        return event;
    }

    public String getSignature() {
        return signature;
    }

    public String getDelivery() {
        return delivery;
    }

    public byte[] getBody() {
        return body;
    }

    public ManagedBot getBot() {
        return bot;
    }

    public void setBot(ManagedBot bot) {
        this.bot = bot;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }
}
