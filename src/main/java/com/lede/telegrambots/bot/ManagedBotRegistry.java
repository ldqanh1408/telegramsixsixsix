package com.lede.telegrambots.bot;

import com.lede.telegrambots.mongo.entity.ManagedBot;
import com.lede.telegrambots.mongo.repo.ManagedBotRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
class ManagedBotRegistry {

    private final ManagedBotRepository bots;
    private final ConcurrentMap<String, ManagedBot> runningBots = new ConcurrentHashMap<>();

    ManagedBotRegistry(ManagedBotRepository bots) {
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

    Optional<ManagedBot> findAny(String username) {
        return bots.findByUsername(BotUsername.of(username).value());
    }

    List<ManagedBot> listBots() {
        return bots.findAll();
    }

    ManagedBot upsert(BotRegistration registration) {
        BotUsername username = BotUsername.of(registration.username());
        if (username.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }

        Optional<ManagedBot> existing = bots.findByUsername(username.value());
        if (registration.tokenOrNull() == null && existing.isEmpty()) {
            throw new IllegalArgumentException("token is required for a new bot");
        }

        ManagedBot saved = bots.save(merge(existing.orElse(null), username.value(), registration));
        if (saved.enabled()) {
            cache(saved);
        } else {
            runningBots.remove(saved.username());
        }
        return saved;
    }

    void delete(String username) {
        String normalized = BotUsername.of(username).value();
        bots.deleteByUsername(normalized);
        runningBots.remove(normalized);
    }

    private ManagedBot cache(ManagedBot bot) {
        runningBots.put(bot.username(), bot);
        return bot;
    }

    private static ManagedBot merge(ManagedBot current, String username, BotRegistration registration) {
        Instant now = Instant.now();
        boolean enabled = registration.enabled() == null || registration.enabled();
        if (current != null && registration.enabled() == null) {
            enabled = current.enabled();
        }
        return new ManagedBot(
                current == null ? null : current.id(),
                username,
                valueOrExisting(registration.tokenOrNull(), current == null ? null : current.token()),
                valueOrExisting(registration.telegramWebhookSecretOrNull(), current == null ? null : current.tgWebhookSecret()),
                valueOrExisting(registration.githubRepoOrNull(), current == null ? null : current.githubRepo()),
                valueOrExisting(registration.githubWebhookSecretOrNull(), current == null ? null : current.ghWebhookSecret()),
                enabled,
                current == null ? now : current.createdAt(),
                now
        );
    }

    private static String valueOrExisting(String incoming, String existing) {
        if (incoming == null) return existing;
        return incoming;
    }
}
