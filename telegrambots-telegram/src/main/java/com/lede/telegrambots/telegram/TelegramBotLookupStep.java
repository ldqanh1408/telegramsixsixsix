package com.lede.telegrambots.telegram;

import com.lede.telegrambots.bot.BotManagementUseCase;
import com.lede.telegrambots.mongo.entity.ManagedBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(1)
class TelegramBotLookupStep implements TelegramWebhookStep {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotLookupStep.class);
    private final BotManagementUseCase bots;

    public TelegramBotLookupStep(BotManagementUseCase bots) {
        this.bots = bots;
    }

    @Override
    public Optional<TelegramWebhookResult> execute(TelegramWebhookContext context) {
        Optional<ManagedBot> maybe = bots.findEnabled(context.getBotUsername());
        if (maybe.isEmpty()) {
            log.warn("Telegram webhook for unknown/disabled bot @{}", context.getBotUsername());
            return Optional.of(TelegramWebhookResult.of(TelegramWebhookResult.Outcome.UNKNOWN_BOT, "unknown bot"));
        }
        context.setBot(maybe.get());
        return Optional.empty();
    }
}
