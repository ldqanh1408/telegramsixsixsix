package com.lede.telegrambots.telegram.impl;

import com.lede.telegrambots.telegram.TelegramWebhookStep;

import com.lede.telegrambots.telegram.*;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(4)
class TelegramCommandParsingStep implements TelegramWebhookStep {

    @Override
    public Optional<TelegramWebhookResult> execute(TelegramWebhookContext context) {
        String text = context.getMessage().text().trim();
        if (!text.startsWith("/")) {
            return Optional.of(TelegramWebhookResult.of(TelegramWebhookResult.Outcome.INVALID_COMMAND, "not a command"));
        }

        String[] parts = text.split("\\s+", 2);
        String key = stripBotSuffix(parts[0]);
        String arg = parts.length > 1 ? parts[1] : "";

        context.setCommandKey(key);
        context.setCommandArg(arg);
        return Optional.empty();
    }

    private static String stripBotSuffix(String token) {
        int at = token.indexOf('@');
        return at < 0 ? token : token.substring(0, at);
    }
}
