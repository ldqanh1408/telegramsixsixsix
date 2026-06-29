package com.lede.telegrambots.domain.bot;

/**
 * Command object describing a desired bot create/update. Blank inputs are normalized to
 * {@code null} so "field omitted" and "field cleared" can be distinguished by callers.
 */
public record BotRegistration(
        String username,
        String tokenOrNull,
        String telegramWebhookSecretOrNull,
        String githubRepoOrNull,
        String githubWebhookSecretOrNull,
        Boolean enabled
) {
    public BotRegistration {
        tokenOrNull = blankToNull(tokenOrNull);
        telegramWebhookSecretOrNull = blankToNull(telegramWebhookSecretOrNull);
        githubRepoOrNull = blankToNull(githubRepoOrNull);
        githubWebhookSecretOrNull = blankToNull(githubWebhookSecretOrNull);
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
