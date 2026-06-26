package com.lede.telegrambots.telegram.command;

/**
 * One Telegram slash command. Implementations are Spring-managed beans;
 * {@link com.lede.telegrambots.telegram.CommandRouter} discovers them by injecting
 * the {@code List<BotCommand>} and indexing by {@link #name()}.
 *
 * <p>Adding a new command = add a new {@code @Component} implementing this interface.
 * Router and existing commands are not touched (Open/Closed Principle).</p>
 */
public interface BotCommand {

    /** Command keyword including the leading slash, e.g. {@code "/start"}. */
    String name();

    /** Execute the command for the given context. Must not throw. */
    void execute(CommandContext ctx);
}
