package com.lede.telegrambots.application.bot.steps;

import com.lede.telegrambots.application.bot.UpsertBotContext;
import com.lede.telegrambots.application.bot.BotUpsertStep;
import com.lede.telegrambots.application.port.out.BotRepository;
import com.lede.telegrambots.domain.bot.ManagedBot;
import java.util.Optional;

public record PersistStep(BotRepository bots) implements BotUpsertStep {
    @Override
    public Optional<ManagedBot> execute(UpsertBotContext ctx) {
        ctx.setSaved(bots.save(ctx.getToSave()));
        return Optional.empty();
    }
}
