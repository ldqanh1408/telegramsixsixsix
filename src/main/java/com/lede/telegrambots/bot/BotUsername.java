package com.lede.telegrambots.bot;

import java.util.Locale;

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
