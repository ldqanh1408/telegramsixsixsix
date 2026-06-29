package com.lede.telegrambots.infrastructure.cache;

import com.lede.telegrambots.application.port.out.BotCache;
import com.lede.telegrambots.application.port.out.BotRepository;
import com.lede.telegrambots.domain.bot.ManagedBot;
import com.lede.telegrambots.domain.shared.BotUsername;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory registry of running enabled bots — the infrastructure adapter for the {@link BotCache}
 * port. Warmed at startup and kept in sync by the bot write use cases; on a miss it falls back to
 * the {@link BotRepository} and caches the result for fast webhook lookups.
 */
@Component
class ManagedBotRegistry implements BotCache {

    private final BotRepository bots;
    private final ConcurrentMap<String, ManagedBot> runningBots = new ConcurrentHashMap<>();

    ManagedBotRegistry(BotRepository bots) {
        this.bots = bots;
    }

    @PostConstruct
    void loadEnabledBots() {
        bots.findAll().stream()
                .filter(ManagedBot::enabled)
                .forEach(this::put);
    }

    @Override
    public Optional<ManagedBot> findEnabled(String username) {
        BotUsername normalized = BotUsername.of(username);
        ManagedBot running = runningBots.get(normalized.value());
        if (running != null && running.enabled()) {
            return Optional.of(running);
        }
        return bots.findByUsername(normalized.value())
                .filter(ManagedBot::enabled)
                .map(bot -> {
                    put(bot);
                    return bot;
                });
    }

    @Override
    public void put(ManagedBot bot) {
        runningBots.put(bot.username(), bot);
    }

    @Override
    public void evict(String username) {
        runningBots.remove(BotUsername.of(username).value());
    }
}
