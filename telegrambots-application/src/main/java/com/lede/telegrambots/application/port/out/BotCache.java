package com.lede.telegrambots.application.port.out;

import com.lede.telegrambots.domain.bot.ManagedBot;

import java.util.Optional;

/**
 * Outbound port for the in-memory registry of enabled bots used for fast webhook lookup.
 * The infrastructure adapter keeps the cache warm and falls back to the {@link BotRepository}.
 */
public interface BotCache {

    /** Resolve an enabled bot by (any-cased, @-prefixed) username, loading + caching on miss. */
    Optional<ManagedBot> findEnabled(String username);

    /** Add or refresh a bot in the cache. */
    void put(ManagedBot bot);

    /** Drop a bot from the cache by username. */
    void evict(String username);
}
