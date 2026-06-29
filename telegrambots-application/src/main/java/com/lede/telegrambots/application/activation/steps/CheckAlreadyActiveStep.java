package com.lede.telegrambots.application.activation.steps;

import com.lede.telegrambots.application.activation.ActivateContext;
import com.lede.telegrambots.application.pipeline.Step;
import com.lede.telegrambots.domain.activation.ActivationResult;
import java.util.Optional;

public class CheckAlreadyActiveStep implements Step<ActivateContext, ActivationResult> {
    @Override
    public Optional<ActivationResult> execute(ActivateContext ctx) {
        if (ctx.getExisting() != null && ctx.getExisting().active()) {
            return Optional.of(new ActivationResult(ctx.getExisting(), false));
        }
        return Optional.empty();
    }
}
