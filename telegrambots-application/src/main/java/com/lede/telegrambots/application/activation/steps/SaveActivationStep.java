package com.lede.telegrambots.application.activation.steps;

import com.lede.telegrambots.application.activation.ActivateContext;
import com.lede.telegrambots.application.pipeline.Step;
import com.lede.telegrambots.application.port.out.ActivationRepository;
import com.lede.telegrambots.domain.activation.ActivationResult;
import com.lede.telegrambots.domain.activation.GroupActivation;
import java.time.Instant;
import java.util.Optional;

public record SaveActivationStep(ActivationRepository store) implements Step<ActivateContext, ActivationResult> {
    @Override
    public Optional<ActivationResult> execute(ActivateContext ctx) {
        Instant now = Instant.now();
        GroupActivation current = ctx.getExisting();
        GroupActivation saved = store.save(new GroupActivation(
                current == null ? null : current.id(),
                ctx.getBot().id(),
                ctx.getBot().username(),
                ctx.getChatId(),
                true,
                current == null || current.activatedAt() == null ? now : current.activatedAt(),
                now
        ));
        return Optional.of(new ActivationResult(saved, true));
    }
}
