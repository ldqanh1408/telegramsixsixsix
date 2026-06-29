package com.lede.telegrambots.application.bot.steps;

import com.lede.telegrambots.application.bot.DeleteBotContext;
import com.lede.telegrambots.application.pipeline.Step;
import com.lede.telegrambots.application.port.out.BotCache;
import java.util.Optional;

public record EvictCacheStep(BotCache cache) implements Step<DeleteBotContext, Boolean> {
    @Override
    public Optional<Boolean> execute(DeleteBotContext ctx) {
        cache.evict(ctx.getUsername());
        return Optional.empty();
    }
}
