package com.lede.telegrambots.application.bot;

import com.lede.telegrambots.domain.bot.ManagedBot;
import com.lede.telegrambots.domain.pipeline.Step;

/**
 * Named step type for the bot upsert workflow.
 */
public interface BotUpsertStep extends Step<UpsertBotContext, ManagedBot> {
}
