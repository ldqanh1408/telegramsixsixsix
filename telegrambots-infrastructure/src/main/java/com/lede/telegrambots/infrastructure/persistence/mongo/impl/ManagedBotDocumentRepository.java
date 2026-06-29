package com.lede.telegrambots.infrastructure.persistence.mongo.impl;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * Spring Data repository over {@link ManagedBotDocument}. Used only by {@link MongoBotRepository},
 * which adapts it to the application's {@code BotRepository} port.
 */
public interface ManagedBotDocumentRepository extends MongoRepository<ManagedBotDocument, String> {

    Optional<ManagedBotDocument> findByUsername(String username);

    void deleteByUsername(String username);
}
