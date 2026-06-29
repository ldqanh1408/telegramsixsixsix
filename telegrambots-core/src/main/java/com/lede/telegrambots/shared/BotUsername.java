package com.lede.telegrambots.shared;

import java.util.Locale;

/**
 * Value object normalizing a Telegram bot username (e.g. {@code @My_Bot} → {@code my_bot}).
 *
 * <p>Lives in the {@code shared} module so any module can reuse it without creating a
 * dependency on another feature module (breaks the former {@code bot} ⇄ {@code activation}
 * cycle).</p>
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
