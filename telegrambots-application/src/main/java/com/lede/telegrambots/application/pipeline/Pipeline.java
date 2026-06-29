package com.lede.telegrambots.application.pipeline;

import java.util.List;
import java.util.Optional;

/**
 * Runs an ordered list of {@link Step}s against a shared context, returning the result of the
 * first step that short-circuits. If no step short-circuits, the result is {@link Optional#empty()}
 * and the caller supplies the fallback.
 *
 * <p>Generic and immutable so it can be reused for any "sequence of guarded stages" — inbound
 * webhook processing and write-side business flows alike.</p>
 *
 * @param <C> context type carried through the stages
 * @param <R> result type a stage may produce to stop the pipeline
 */
public final class Pipeline<C, R> {

    private final List<? extends Step<C, R>> steps;

    public Pipeline(List<? extends Step<C, R>> steps) {
        this.steps = List.copyOf(steps);
    }

    /**
     * Executes each step in order; the first non-empty result wins and stops the pipeline.
     *
     * @return the short-circuit result, or {@link Optional#empty()} if every step proceeded
     */
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
