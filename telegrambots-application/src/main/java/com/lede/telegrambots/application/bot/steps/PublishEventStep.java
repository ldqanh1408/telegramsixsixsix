package com.lede.telegrambots.application.bot.steps;

import com.lede.telegrambots.application.bot.UpsertBotContext;
import com.lede.telegrambots.application.pipeline.Step;
import com.lede.telegrambots.application.port.out.DomainEventPublisher;
import com.lede.telegrambots.domain.bot.BotSavedEvent;
import com.lede.telegrambots.domain.bot.ManagedBot;
import java.util.Optional;

public record PublishEventStep(DomainEventPublisher events) implements Step<UpsertBotContext, ManagedBot> {
    @Override
    public Optional<ManagedBot> execute(UpsertBotContext ctx) {
        ManagedBot saved = ctx.getSaved();
        events.publish(new BotSavedEvent(saved.username(), saved.token(), saved.tgWebhookSecret(), saved.enabled()));
        return Optional.of(saved);
    }
}
