package com.lede.telegrambots.telegram.impl;

/**
 * Result of processing a Telegram webhook update.
 */
public record TelegramWebhookResult(Outcome outcome, String message) {
    public enum Outcome {
        OK,
        UNKNOWN_BOT,
        BAD_SECRET,
        INVALID_MESSAGE,
        INVALID_COMMAND,
        UNKNOWN_COMMAND,
        EXECUTION_ERROR
    }

    public static TelegramWebhookResult of(Outcome outcome, String message) {
        return new TelegramWebhookResult(outcome, message);
    }
}
