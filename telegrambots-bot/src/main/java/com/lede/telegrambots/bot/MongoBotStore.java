package com.lede.telegrambots.bot;

import com.lede.telegrambots.mongo.entity.ManagedBot;
import com.lede.telegrambots.mongo.repo.ManagedBotRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Mongo adapter for {@link BotStore}: the only class in the bot module that touches the
 * Spring Data repository. Pure delegation.
 */
@Component
class MongoBotStore implements BotStore {

    private final ManagedBotRepository repo;

    MongoBotStore(ManagedBotRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<ManagedBot> findAll() {
        return repo.findAll();
    }

    @Override
    public Optional<ManagedBot> findByUsername(String username) {
        return repo.findByUsername(username);
    }

    @Override
    public ManagedBot save(ManagedBot bot) {
        return repo.save(bot);
    }

    @Override
    public void deleteByUsername(String username) {
        repo.deleteByUsername(username);
    }
}
