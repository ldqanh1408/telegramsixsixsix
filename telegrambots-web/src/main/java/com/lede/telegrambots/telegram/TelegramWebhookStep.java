package com.lede.telegrambots.telegram;

import com.lede.telegrambots.domain.pipeline.Step;
import com.lede.telegrambots.telegram.impl.TelegramWebhookContext;
import com.lede.telegrambots.telegram.impl.TelegramWebhookResult;

/**
 * A single stage of the Telegram webhook pipeline. A typed alias over the shared
 * {@link Step} abstraction so the telegram steps form a distinct, generic-aware bean group
 * that Spring can inject as a {@code List<TelegramWebhookStep>}.
 */
public interface TelegramWebhookStep extends Step<TelegramWebhookContext, TelegramWebhookResult> {
}
