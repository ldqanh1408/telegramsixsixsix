package com.lede.telegrambots.telegram.steps;

import com.lede.telegrambots.telegram.TelegramWebhookStep;
import com.lede.telegrambots.telegram.TelegramWebhookContext;
import com.lede.telegrambots.telegram.TelegramWebhookResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(2)
public class TelegramSecretVerificationStep implements TelegramWebhookStep {

    private static final Logger log = LoggerFactory.getLogger(TelegramSecretVerificationStep.class);

    @Override
    public Optional<TelegramWebhookResult> execute(TelegramWebhookContext context) {
        String expected = context.getBot().tgWebhookSecret();
        String received = context.getSecret();
        if (expected != null && !expected.isBlank() && !expected.equals(received)) {
            log.warn("Rejected Telegram webhook for @{}: bad/missing secret token", context.getBotUsername());
            return Optional.of(TelegramWebhookResult.of(TelegramWebhookResult.Outcome.BAD_SECRET, "bad secret"));
        }
        return Optional.empty();
    }
}
