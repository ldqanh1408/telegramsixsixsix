package com.lede.telegrambots.application.activation.steps;

import com.lede.telegrambots.application.activation.DeactivateContext;
import com.lede.telegrambots.application.activation.DeactivateGroupStep;
import com.lede.telegrambots.application.port.out.ActivationRepository;
import java.util.Optional;

public record LoadExistingActivationForDeactivateStep(ActivationRepository store) implements DeactivateGroupStep {
    @Override
    public Optional<Boolean> execute(DeactivateContext ctx) {
        ctx.setExisting(store.find(ctx.getBot().username(), ctx.getChatId()).orElse(null));
        return Optional.empty();
    }
}
