package com.lede.telegrambots.application.pipeline;

import java.util.Optional;

/**
 * A single stage of a {@link Pipeline}.
 *
 * <p>A step inspects/mutates the shared context {@code C} and either lets the pipeline continue
 * ({@link Optional#empty()}) or short-circuits it by returning a result {@code R}. This one
 * abstraction backs both the inbound webhook pipelines (GitHub/Telegram) and the write-side
 * business use cases (bot upsert/delete), so the "chain of stages" pattern is expressed once.</p>
 *
 * @param <C> mutable context carried through the pipeline
 * @param <R> result type produced when a step decides to short-circuit
 */
@FunctionalInterface
public interface Step<C, R> {

    /**
     * @param context shared pipeline context
     * @return a present result to stop the pipeline and return it; {@link Optional#empty()} to proceed
     */
    Optional<R> execute(C context);
}
