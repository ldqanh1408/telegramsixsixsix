package com.lede.telegrambots.domain.activation;

import com.lede.telegrambots.domain.pipeline.Pipeline;
import com.lede.telegrambots.domain.pipeline.Step;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class GroupActivation {
    private String id;
    private String botId;
    private String botUsername;
    private long chatId;
    private boolean active;
    private Instant activatedAt;
    private Instant updatedAt;

    private static final Pipeline<ValidationContext, String> validationPipeline = new Pipeline<>(List.of(
            new ValidateBotIdStep(),
            new ValidateBotUsernameStep(),
            new ValidateChatIdStep()
    ));

    private record ValidationContext(String botId, String botUsername, long chatId) {}

    private static class ValidateBotIdStep implements Step<ValidationContext, String> {
        @Override
        public Optional<String> execute(ValidationContext ctx) {
            if (ctx.botId() == null || ctx.botId().isBlank()) {
                return Optional.of("botId is required");
            }
            return Optional.empty();
        }
    }

    private static class ValidateBotUsernameStep implements Step<ValidationContext, String> {
        @Override
        public Optional<String> execute(ValidationContext ctx) {
            if (ctx.botUsername() == null || ctx.botUsername().isBlank()) {
                return Optional.of("botUsername is required");
            }
            return Optional.empty();
        }
    }

    private static class ValidateChatIdStep implements Step<ValidationContext, String> {
        @Override
        public Optional<String> execute(ValidationContext ctx) {
            if (ctx.chatId() == 0) {
                return Optional.of("chatId cannot be zero");
            }
            return Optional.empty();
        }
    }

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
        validationPipeline.run(new ValidationContext(botId, botUsername, chatId))
                .ifPresent(error -> {
                    throw new IllegalArgumentException(error);
                });
    }
}
