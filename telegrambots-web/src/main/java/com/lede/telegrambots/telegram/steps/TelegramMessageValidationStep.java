package com.lede.telegrambots.telegram.steps;

import com.lede.telegrambots.telegram.TelegramWebhookStep;
import com.lede.telegrambots.telegram.impl.TelegramWebhookContext;
import com.lede.telegrambots.telegram.impl.TelegramWebhookResult;

import com.lede.telegrambots.telegram.TelegramWebhookStep;

import com.lede.telegrambots.telegram.*;

import com.lede.telegrambots.telegram.dto.Update;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(3)
public class TelegramMessageValidationStep implements TelegramWebhookStep {

    @Override
    public Optional<TelegramWebhookResult> execute(TelegramWebhookContext context) {
        Update.Message msg = context.getUpdate().message();
        if (msg == null || msg.text() == null) {
            return Optional.of(TelegramWebhookResult.of(TelegramWebhookResult.Outcome.INVALID_MESSAGE, "missing message or text"));
        }
        context.setMessage(msg);
        context.setChatId(msg.chat().id());
        return Optional.empty();
    }
}
