package com.lede.telegrambots.application.notification;

import com.lede.telegrambots.domain.pipeline.Step;

/**
 * Named step type for the notification broadcast workflow.
 */
public interface BroadcastStep extends Step<BroadcastContext, Boolean> {
}
