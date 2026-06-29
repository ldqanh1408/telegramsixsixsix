package com.lede.telegrambots.application.bot;

import com.lede.telegrambots.domain.pipeline.Step;

/**
 * Named step type for the bot deletion workflow.
 */
public interface BotDeleteStep extends Step<DeleteBotContext, Boolean> {
}
