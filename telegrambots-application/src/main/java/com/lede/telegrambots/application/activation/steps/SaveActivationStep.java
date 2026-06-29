package com.lede.telegrambots.application.activation.steps;

import com.lede.telegrambots.application.activation.ActivateContext;
import com.lede.telegrambots.application.activation.ActivateGroupStep;
import com.lede.telegrambots.application.port.out.ActivationRepository;
import com.lede.telegrambots.domain.activation.ActivationResult;
import com.lede.telegrambots.domain.activation.GroupActivation;
import java.time.Instant;
import java.util.Optional;

public record SaveActivationStep(ActivationRepository store) implements ActivateGroupStep {
    @Override
    public Optional<ActivationResult> execute(ActivateContext ctx) {
        GroupActivation current = ctx.getExisting();
        if (current == null) {
            Instant now = Instant.now();
            current = new GroupActivation(
                    null,
                    ctx.getBot().id(),
                    ctx.getBot().username(),
                    ctx.getChatId(),
                    true,
                    now,
                    now
            );
        } else {
            current.activate();
        }
        GroupActivation saved = store.save(current);
        return Optional.of(new ActivationResult(saved, true));
    }
}
