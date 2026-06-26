package com.lede.telegrambots.telegram;

import java.util.Optional;

/**
 * Functional interface for a single step in the Telegram webhook processing pipeline.
 */
@FunctionalInterface
interface TelegramWebhookStep {

    /**
     * Executes the step's logic.
     *
     * @param context the context holding input parameters and intermediate/shared data
     * @return Optional containing the result if the step wishes to short-circuit the pipeline,
     *         or Optional.empty() if it wishes to proceed to the next step
     */
    Optional<TelegramWebhookResult> execute(TelegramWebhookContext context);
}
