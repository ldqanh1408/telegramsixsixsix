package com.lede.telegrambots.application.activation.steps;

import com.lede.telegrambots.application.activation.ActivateContext;
import com.lede.telegrambots.application.pipeline.Step;
import com.lede.telegrambots.application.port.out.ActivationRepository;
import com.lede.telegrambots.domain.activation.ActivationResult;
import java.util.Optional;

public record LoadExistingActivationForActivateStep(ActivationRepository store) implements Step<ActivateContext, ActivationResult> {
    @Override
    public Optional<ActivationResult> execute(ActivateContext ctx) {
        ctx.setExisting(store.find(ctx.getBot().username(), ctx.getChatId()).orElse(null));
        return Optional.empty();
    }
}
