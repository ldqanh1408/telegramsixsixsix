package com.lede.telegrambots.infrastructure.persistence.mongo.impl;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB persistence model for a group activation. Framework-coupled twin of the pure domain
 * entity {@code GroupActivation}; {@link GroupActivationMapper} translates between the two.
 */
@Document(collection = "group_activations")
@CompoundIndexes({
        @CompoundIndex(name = "bot_chat_unique",
                def = "{'botUsername': 1, 'chatId': 1}",
                unique = true)
})
public record GroupActivationDocument(
        @Id String id,
        @Indexed String botId,
        @Indexed String botUsername,
        long chatId,
        boolean active,
        Instant activatedAt,
        Instant updatedAt
) {
}
