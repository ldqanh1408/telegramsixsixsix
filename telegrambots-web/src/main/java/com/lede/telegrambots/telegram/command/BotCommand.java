package com.lede.telegrambots.telegram.command;

import java.util.Optional;

/**
 * One Telegram slash command. Implementations are Spring-managed beans;
 * {@link com.lede.telegrambots.telegram.CommandRouter} discovers them by injecting
 * the {@code List<BotCommand>} and indexing by {@link #name()}.
 *
 * <p>Commands are pure: they map a {@link CommandContext} to an optional reply and
 * never touch the transport. The router (the invoker) performs the single send. This
 * keeps every command free of any HTTP/{@code TelegramClient} dependency, so adding a
 * command never widens the coupling surface (Open/Closed Principle).</p>
 */
public interface BotCommand {

    /** Command keyword including the leading slash, e.g. {@code "/start"}. */
    String name();

    /**
     * Produce the reply for the given context.
     *
     * @return the HTML reply to send, or {@link Optional#empty()} to stay silent
     *         (e.g. the command targets a different bot). Must not throw.
     */
    Optional<String> execute(CommandContext ctx);
}
