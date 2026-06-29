package com.lede.telegrambots.application.activation.steps;

import com.lede.telegrambots.application.activation.DeactivateContext;
import com.lede.telegrambots.application.activation.DeactivateGroupStep;
import com.lede.telegrambots.application.port.out.ActivationRepository;
import com.lede.telegrambots.domain.activation.GroupActivation;
import java.util.Optional;

public record SaveDeactivatedStep(ActivationRepository store) implements DeactivateGroupStep {
    @Override
    public Optional<Boolean> execute(DeactivateContext ctx) {
        GroupActivation current = ctx.getExisting();
        current.deactivate();
        store.save(current);
        return Optional.of(Boolean.TRUE);
    }
}
