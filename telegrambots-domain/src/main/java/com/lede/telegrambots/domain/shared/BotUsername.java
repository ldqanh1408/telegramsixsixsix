package com.lede.telegrambots.domain.shared;

import java.util.Locale;

/**
 * Value object normalizing a Telegram bot username (e.g. {@code @My_Bot} → {@code my_bot}).
 *
 * <p>Pure enterprise vocabulary — lives in the domain layer so every other layer can reuse it
 * without anyone depending on a feature package.</p>
 */
public record BotUsername(String value) {

    public BotUsername {
        value = normalize(value);
    }

    public boolean isBlank() {
        return value.isBlank();
    }

    public static BotUsername of(String username) {
        return new BotUsername(username);
    }

    private static String normalize(String username) {
        if (username == null) return "";
        String normalized = username.trim();
        while (normalized.startsWith("@")) {
            normalized = normalized.substring(1);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }
}
