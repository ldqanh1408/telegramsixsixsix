package com.lede.telegrambots.application.activation.steps;

import com.lede.telegrambots.application.activation.DeactivateContext;
import com.lede.telegrambots.application.pipeline.Step;
import java.util.Optional;

public class CheckNotActiveStep implements Step<DeactivateContext, Boolean> {
    @Override
    public Optional<Boolean> execute(DeactivateContext ctx) {
        if (ctx.getExisting() == null || !ctx.getExisting().active()) {
            return Optional.of(Boolean.FALSE);
        }
        return Optional.empty();
    }
}
