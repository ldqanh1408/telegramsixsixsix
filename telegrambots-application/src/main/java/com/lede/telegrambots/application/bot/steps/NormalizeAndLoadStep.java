package com.lede.telegrambots.application.bot.steps;

import com.lede.telegrambots.application.bot.UpsertBotContext;
import com.lede.telegrambots.application.bot.BotUpsertStep;
import com.lede.telegrambots.application.port.out.BotRepository;
import com.lede.telegrambots.domain.bot.ManagedBot;
import com.lede.telegrambots.domain.shared.BotUsername;
import java.util.Optional;

public record NormalizeAndLoadStep(BotRepository bots) implements BotUpsertStep {
    @Override
    public Optional<ManagedBot> execute(UpsertBotContext ctx) {
        ctx.setUsername(BotUsername.of(ctx.getRegistration().username()).value());
        ctx.setExisting(bots.findByUsername(ctx.getUsername()).orElse(null));
        return Optional.empty();
    }
}
