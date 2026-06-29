package com.lede.telegrambots.domain.pipeline;

import java.util.List;
import java.util.Optional;

public class Pipeline<C, R> {

    private final List<? extends Step<C, R>> steps;

    public Pipeline(List<? extends Step<C, R>> steps) {
        this.steps = steps != null ? List.copyOf(steps) : List.of();
    }

    public Optional<R> run(C context) {
        for (Step<C, R> step : steps) {
            Optional<R> result = step.execute(context);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }
}
