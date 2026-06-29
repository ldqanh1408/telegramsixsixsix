package com.lede.telegrambots.application.bot.steps;

import com.lede.telegrambots.application.bot.DeleteBotContext;
import com.lede.telegrambots.application.bot.BotDeleteStep;
import com.lede.telegrambots.application.port.out.DomainEventPublisher;
import com.lede.telegrambots.domain.bot.BotDeletedEvent;
import java.util.Optional;

public record PublishDeleteEventStep(DomainEventPublisher events) implements BotDeleteStep {
    @Override
    public Optional<Boolean> execute(DeleteBotContext ctx) {
        if (ctx.getExisting() != null) {
            events.publish(new BotDeletedEvent(ctx.getExisting().username(), ctx.getExisting().token()));
        }
        return Optional.of(Boolean.TRUE);
    }
}
