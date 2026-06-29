package com.lede.telegrambots.infrastructure.persistence.mongo.impl;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository over {@link GroupActivationDocument}. Used only by
 * {@link MongoActivationRepository}, which adapts it to the application's
 * {@code ActivationRepository} port.
 */
public interface GroupActivationDocumentRepository extends MongoRepository<GroupActivationDocument, String> {

    Optional<GroupActivationDocument> findByBotUsernameAndChatId(String botUsername, long chatId);

    List<GroupActivationDocument> findByBotUsernameAndActiveTrue(String botUsername);

    long countByBotUsernameAndActiveTrue(String botUsername);

    boolean existsByBotUsernameAndChatIdAndActiveTrue(String botUsername, long chatId);

    void deleteByBotUsername(String botUsername);
}
