package com.lede.telegrambots.telegram.command;

import com.lede.telegrambots.domain.bot.ManagedBot;

/**
 * Immutable input for a {@link BotCommand} execution.
 *
 * @param chatId Telegram chat where the command was issued
 * @param arg    raw argument string after the command keyword (never null, may be empty)
 * @param bot    the {@link ManagedBot} owning this webhook URL — every command resolves
 *               its token, repo and identity through this reference (no global config)
 */
public record CommandContext(long chatId, String arg, ManagedBot bot) {
    public CommandContext {
        if (arg == null) arg = "";
        if (bot == null) throw new IllegalArgumentException("bot must not be null");
    }
}
