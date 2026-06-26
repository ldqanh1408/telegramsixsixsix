package com.lede.telegrambots.bot;

import com.lede.telegrambots.mongo.entity.ManagedBot;
import com.lede.telegrambots.shared.BotUsername;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory registry (cache) for running enabled bots.
 * Keeps active bots cached in memory for fast webhook lookup.
 */
@Service
class ManagedBotRegistry {

    private final BotStore bots;
    private final ConcurrentMap<String, ManagedBot> runningBots = new ConcurrentHashMap<>();

    ManagedBotRegistry(BotStore bots) {
        this.bots = bots;
    }

    @PostConstruct
    void loadEnabledBots() {
        bots.findAll().stream()
                .filter(ManagedBot::enabled)
                .forEach(this::cache);
    }

    Optional<ManagedBot> findEnabled(String username) {
        BotUsername normalized = BotUsername.of(username);
        ManagedBot running = runningBots.get(normalized.value());
        if (running != null && running.enabled()) {
            return Optional.of(running);
        }
        return bots.findByUsername(normalized.value())
                .filter(ManagedBot::enabled)
                .map(this::cache);
    }

    ManagedBot cache(ManagedBot bot) {
        runningBots.put(bot.username(), bot);
        return bot;
    }

    void remove(String username) {
        runningBots.remove(BotUsername.of(username).value());
    }
}
