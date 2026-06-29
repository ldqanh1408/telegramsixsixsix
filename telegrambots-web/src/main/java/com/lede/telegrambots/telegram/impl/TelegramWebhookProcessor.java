package com.lede.telegrambots.telegram.impl;

import com.lede.telegrambots.telegram.impl.TelegramWebhookResult.Outcome;

import com.lede.telegrambots.telegram.TelegramWebhookStep;

import com.lede.telegrambots.telegram.*;

import com.lede.telegrambots.application.pipeline.Pipeline;

import com.lede.telegrambots.telegram.dto.Update;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates the Telegram webhook update flow by running the ordered {@link TelegramWebhookStep}
 * beans through the shared {@link Pipeline}. The first step that short-circuits wins; otherwise the
 * default success is returned.
 */
@Service
public class TelegramWebhookProcessor {

    private final Pipeline<TelegramWebhookContext, TelegramWebhookResult> pipeline;

    public TelegramWebhookProcessor(List<TelegramWebhookStep> steps) {
        this.pipeline = new Pipeline<>(steps);
    }

    public TelegramWebhookResult process(String botUsername, String secret, Update update) {
        TelegramWebhookContext context = new TelegramWebhookContext(botUsername, secret, update);

        // Default fallback (normally short-circuited by TelegramCommandExecutionStep)
        return pipeline.run(context)
                .orElseGet(() -> TelegramWebhookResult.of(Outcome.OK, "ok"));
    }
}
