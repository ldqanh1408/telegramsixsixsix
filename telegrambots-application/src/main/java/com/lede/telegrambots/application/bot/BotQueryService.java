package com.lede.telegrambots.application.bot;

import com.lede.telegrambots.application.port.out.BotRepository;
import com.lede.telegrambots.domain.bot.ManagedBot;
import com.lede.telegrambots.domain.shared.BotUsername;

import java.util.List;
import java.util.Optional;

/**
 * Read-side use cases for managed bots. Kept deliberately simple (no pipeline) — these are plain
 * queries with no multi-step business rules.
 */
public class BotQueryService {

    private final BotRepository bots;

    public BotQueryService(BotRepository bots) {
        this.bots = bots;
    }

    public Optional<ManagedBot> findAny(String username) {
        return bots.findByUsername(BotUsername.of(username).value());
    }

    public List<ManagedBot> listBots() {
        return bots.findAll();
    }
}
