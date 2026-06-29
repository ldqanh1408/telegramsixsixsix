package com.lede.telegrambots.domain.pipeline;

import java.util.Optional;

public interface Step<C, R> {
    Optional<R> execute(C context);
}
