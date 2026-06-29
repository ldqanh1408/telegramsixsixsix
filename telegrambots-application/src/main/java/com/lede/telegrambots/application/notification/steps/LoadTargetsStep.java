package com.lede.telegrambots.application.notification.steps;

import com.lede.telegrambots.application.notification.BroadcastContext;
import com.lede.telegrambots.application.pipeline.Step;
import com.lede.telegrambots.application.port.out.ActivationRepository;
import java.util.Optional;

public record LoadTargetsStep(ActivationRepository activations) implements Step<BroadcastContext, Boolean> {
    @Override
    public Optional<Boolean> execute(BroadcastContext ctx) {
        ctx.setTargets(activations.findActive(ctx.getBot().username()));
        return Optional.empty();
    }
}
