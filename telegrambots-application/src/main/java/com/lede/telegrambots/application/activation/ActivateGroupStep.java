package com.lede.telegrambots.application.activation;

import com.lede.telegrambots.domain.activation.ActivationResult;
import com.lede.telegrambots.domain.pipeline.Step;

/**
 * Named step type for the group activation workflow.
 */
public interface ActivateGroupStep extends Step<ActivateContext, ActivationResult> {
}
