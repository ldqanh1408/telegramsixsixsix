package com.lede.telegrambots.infrastructure.persistence.mongo;

import com.lede.telegrambots.infrastructure.persistence.mongo.impl.*;

import com.lede.telegrambots.application.port.out.BotRepository;
import com.lede.telegrambots.domain.bot.ManagedBot;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Mongo adapter implementing the application's {@link BotRepository} port. The only place in the
 * codebase that touches the Spring Data bot repository; maps documents to/from the domain entity.
 */
@Component
class MongoBotRepository implements BotRepository {

    private final ManagedBotDocumentRepository repo;

    MongoBotRepository(ManagedBotDocumentRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<ManagedBot> findAll() {
        return repo.findAll().stream().map(ManagedBotMapper::toDomain).toList();
    }

    @Override
    public Optional<ManagedBot> findByUsername(String username) {
        return repo.findByUsername(username).map(ManagedBotMapper::toDomain);
    }

    @Override
    public ManagedBot save(ManagedBot bot) {
        return ManagedBotMapper.toDomain(repo.save(ManagedBotMapper.toDocument(bot)));
    }

    @Override
    public void deleteByUsername(String username) {
        repo.deleteByUsername(username);
    }
}
