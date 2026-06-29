package com.lede.telegrambots.application.bot.steps;

import com.lede.telegrambots.application.bot.DeleteBotContext;
import com.lede.telegrambots.application.pipeline.Step;
import com.lede.telegrambots.application.port.out.ActivationRepository;
import java.util.Optional;

public record DeleteActivationsStep(ActivationRepository activations) implements Step<DeleteBotContext, Boolean> {
    @Override
    public Optional<Boolean> execute(DeleteBotContext ctx) {
        activations.deleteAllFor(ctx.getUsername());
        return Optional.empty();
    }
}
