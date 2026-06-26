package com.lede.telegrambots.activation;

import com.lede.telegrambots.mongo.entity.GroupActivation;
import com.lede.telegrambots.mongo.repo.GroupActivationRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Mongo adapter for {@link ActivationStore}: the only class in the activation module that
 * touches the Spring Data repository. Pure delegation — no business rules here.
 */
@Component
class MongoActivationStore implements ActivationStore {

    private final GroupActivationRepository repo;

    MongoActivationStore(GroupActivationRepository repo) {
        this.repo = repo;
    }

    @Override
    public Optional<GroupActivation> find(String botUsername, long chatId) {
        return repo.findByBotUsernameAndChatId(botUsername, chatId);
    }

    @Override
    public GroupActivation save(GroupActivation activation) {
        return repo.save(activation);
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
        return repo.findByBotUsernameAndActiveTrue(botUsername);
    }

    @Override
    public void deleteAllFor(String botUsername) {
        repo.deleteByBotUsername(botUsername);
    }
}
