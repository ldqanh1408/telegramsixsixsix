package com.lede.telegrambots.application.activation.steps;

import com.lede.telegrambots.application.activation.DeactivateContext;
import com.lede.telegrambots.application.pipeline.Step;
import com.lede.telegrambots.application.port.out.ActivationRepository;
import com.lede.telegrambots.domain.activation.GroupActivation;
import java.time.Instant;
import java.util.Optional;

public record SaveDeactivatedStep(ActivationRepository store) implements Step<DeactivateContext, Boolean> {
    @Override
    public Optional<Boolean> execute(DeactivateContext ctx) {
        GroupActivation current = ctx.getExisting();
        store.save(new GroupActivation(
                current.id(),
                current.botId(),
                current.botUsername(),
                current.chatId(),
                false,
                current.activatedAt(),
                Instant.now()
        ));
        return Optional.of(Boolean.TRUE);
    }
}
