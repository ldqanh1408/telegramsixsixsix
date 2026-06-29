package com.lede.telegrambots.application.bot.steps;

import com.lede.telegrambots.application.bot.UpsertBotContext;
import com.lede.telegrambots.application.bot.BotUpsertStep;
import com.lede.telegrambots.domain.bot.ManagedBot;
import java.util.Optional;

public record MergeStep() implements BotUpsertStep {
    @Override
    public Optional<ManagedBot> execute(UpsertBotContext ctx) {
        ManagedBot existing = ctx.getExisting();
        if (existing == null) {
            ctx.setToSave(ManagedBot.create(ctx.getUsername(), ctx.getRegistration()));
        } else {
            existing.merge(ctx.getRegistration());
            ctx.setToSave(existing);
        }
        return Optional.empty();
    }
}
