package com.lede.telegrambots.application.activation;

import com.lede.telegrambots.domain.pipeline.Step;

/**
 * Named step type for the group deactivation workflow.
 */
public interface DeactivateGroupStep extends Step<DeactivateContext, Boolean> {
}
