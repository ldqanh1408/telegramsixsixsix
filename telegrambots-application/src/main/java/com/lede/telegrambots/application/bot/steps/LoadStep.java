package com.lede.telegrambots.application.bot.steps;

import com.lede.telegrambots.application.bot.DeleteBotContext;
import com.lede.telegrambots.application.bot.BotDeleteStep;
import com.lede.telegrambots.application.port.out.BotRepository;
import java.util.Optional;

public record LoadStep(BotRepository bots) implements BotDeleteStep {
    @Override
    public Optional<Boolean> execute(DeleteBotContext ctx) {
        ctx.setExisting(bots.findByUsername(ctx.getUsername()).orElse(null));
        return Optional.empty();
    }
}
