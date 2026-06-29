package com.lede.telegrambots.domain.activation;

import java.time.Instant;

public class GroupActivation {
    private String id;
    private String botId;
    private String botUsername;
    private long chatId;
    private boolean active;
    private Instant activatedAt;
    private Instant updatedAt;

    public GroupActivation(
            String id,
            String botId,
            String botUsername,
            long chatId,
            boolean active,
            Instant activatedAt,
            Instant updatedAt
    ) {
        requireValid(botId, botUsername, chatId);

        this.id = id;
        this.botId = botId;
        this.botUsername = botUsername;
        this.chatId = chatId;
        this.active = active;
        this.activatedAt = activatedAt;
        this.updatedAt = updatedAt;
    }

    public String id() { return id; }
    public String botId() { return botId; }
    public String botUsername() { return botUsername; }
    public long chatId() { return chatId; }
    public boolean active() { return active; }
    public Instant activatedAt() { return activatedAt; }
    public Instant updatedAt() { return updatedAt; }

    public void activate() {
        this.active = true;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    private static void requireValid(String botId, String botUsername, long chatId) {
        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("botId is required");
        }
        if (botUsername == null || botUsername.isBlank()) {
            throw new IllegalArgumentException("botUsername is required");
        }
        if (chatId == 0) {
            throw new IllegalArgumentException("chatId cannot be zero");
        }
    }
}
