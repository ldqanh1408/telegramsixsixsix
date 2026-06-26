package com.lede.telegrambots.telegram;

import com.lede.telegrambots.telegram.TelegramWebhookResult.Outcome;
import com.lede.telegrambots.telegram.dto.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Application service that orchestrates the Telegram webhook update processing pipeline
 * using the Component-based Pipeline Pattern (Spring Beans + @Order).
 */
@Service
public class TelegramWebhookProcessor {

    private static final Logger log = LoggerFactory.getLogger(TelegramWebhookProcessor.class);

    private final List<TelegramWebhookStep> steps;

    public TelegramWebhookProcessor(List<TelegramWebhookStep> steps) {
        this.steps = steps;
    }

    public TelegramWebhookResult process(String botUsername, String secret, Update update) {
        TelegramWebhookContext context = new TelegramWebhookContext(botUsername, secret, update);

        for (TelegramWebhookStep step : steps) {
            Optional<TelegramWebhookResult> shortCircuit = step.execute(context);
            if (shortCircuit.isPresent()) {
                return shortCircuit.get();
            }
        }

        // Default fallback (normally short-circuited by TelegramCommandExecutionStep)
        return TelegramWebhookResult.of(Outcome.OK, "ok");
    }
}
