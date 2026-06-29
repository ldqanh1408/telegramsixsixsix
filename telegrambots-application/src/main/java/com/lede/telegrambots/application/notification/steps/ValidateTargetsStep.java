package com.lede.telegrambots.application.notification.steps;

import com.lede.telegrambots.application.notification.BroadcastContext;
import com.lede.telegrambots.application.notification.BroadcastStep;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Optional;

public class ValidateTargetsStep implements BroadcastStep {
    private static final Logger log = System.getLogger(ValidateTargetsStep.class.getName());

    @Override
    public Optional<Boolean> execute(BroadcastContext ctx) {
        if (ctx.getTargets() == null || ctx.getTargets().isEmpty()) {
            log.log(Level.INFO, "Message for @{0} but no activated groups - dropped", ctx.getBot().username());
            return Optional.of(Boolean.FALSE);
        }
        return Optional.empty();
    }
}
