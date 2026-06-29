package com.lede.telegrambots.application.bot.steps;

import com.lede.telegrambots.application.bot.UpsertBotContext;
import com.lede.telegrambots.application.pipeline.Step;
import com.lede.telegrambots.application.port.out.BotCache;
import com.lede.telegrambots.domain.bot.ManagedBot;
import java.util.Optional;

public record RefreshCacheStep(BotCache cache) implements Step<UpsertBotContext, ManagedBot> {
    @Override
    public Optional<ManagedBot> execute(UpsertBotContext ctx) {
        if (ctx.getSaved().enabled()) {
            cache.put(ctx.getSaved());
        } else {
            cache.evict(ctx.getSaved().username());
        }
        return Optional.empty();
    }
}
