package com.lede.telegrambots.application.bot.steps;

import com.lede.telegrambots.application.bot.UpsertBotContext;
import com.lede.telegrambots.application.pipeline.Step;
import com.lede.telegrambots.domain.bot.BotDomainService;
import com.lede.telegrambots.domain.bot.ManagedBot;
import java.util.Optional;

public record MergeStep(BotDomainService domainService) implements Step<UpsertBotContext, ManagedBot> {
    @Override
    public Optional<ManagedBot> execute(UpsertBotContext ctx) {
        ctx.setToSave(domainService.prepareForSave(ctx.getExisting(), ctx.getUsername(), ctx.getRegistration()));
        return Optional.empty();
    }
}
