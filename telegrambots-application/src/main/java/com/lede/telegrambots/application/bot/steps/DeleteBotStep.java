package com.lede.telegrambots.application.bot.steps;

import com.lede.telegrambots.application.bot.DeleteBotContext;
import com.lede.telegrambots.application.bot.BotDeleteStep;
import com.lede.telegrambots.application.port.out.BotRepository;
import java.util.Optional;

public record DeleteBotStep(BotRepository bots) implements BotDeleteStep {
    @Override
    public Optional<Boolean> execute(DeleteBotContext ctx) {
        bots.deleteByUsername(ctx.getUsername());
        return Optional.empty();
    }
}
