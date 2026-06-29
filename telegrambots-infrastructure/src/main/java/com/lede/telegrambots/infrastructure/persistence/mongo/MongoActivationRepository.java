package com.lede.telegrambots.infrastructure.persistence.mongo;

import com.lede.telegrambots.infrastructure.persistence.mongo.impl.*;

import com.lede.telegrambots.application.port.out.ActivationRepository;
import com.lede.telegrambots.domain.activation.GroupActivation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Mongo adapter implementing the application's {@link ActivationRepository} port. Pure delegation
 * plus document/domain mapping — no business rules here.
 */
@Component
class MongoActivationRepository implements ActivationRepository {

    private final GroupActivationDocumentRepository repo;

    MongoActivationRepository(GroupActivationDocumentRepository repo) {
        this.repo = repo;
    }

    @Override
    public Optional<GroupActivation> find(String botUsername, long chatId) {
        return repo.findByBotUsernameAndChatId(botUsername, chatId).map(GroupActivationMapper::toDomain);
    }

    @Override
    public GroupActivation save(GroupActivation activation) {
        return GroupActivationMapper.toDomain(repo.save(GroupActivationMapper.toDocument(activation)));
    }

    @Override
    public boolean isActive(String botUsername, long chatId) {
        return repo.existsByBotUsernameAndChatIdAndActiveTrue(botUsername, chatId);
    }

    @Override
    public long countActive(String botUsername) {
        return repo.countByBotUsernameAndActiveTrue(botUsername);
    }

    @Override
    public List<GroupActivation> findActive(String botUsername) {
        return repo.findByBotUsernameAndActiveTrue(botUsername).stream()
                .map(GroupActivationMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteAllFor(String botUsername) {
        repo.deleteByBotUsername(botUsername);
    }
}
