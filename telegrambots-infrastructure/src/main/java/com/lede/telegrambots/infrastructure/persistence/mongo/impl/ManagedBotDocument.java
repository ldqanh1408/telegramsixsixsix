package com.lede.telegrambots.infrastructure.persistence.mongo.impl;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB persistence model for a managed bot. This is the framework-coupled twin of the pure
 * domain entity {@code ManagedBot}; {@link ManagedBotMapper} translates between the two so the
 * domain stays free of Spring Data annotations.
 */
@Document(collection = "managed_bots")
public record ManagedBotDocument(
        @Id String id,
        @Indexed(unique = true) String username,
        String token,
        String tgWebhookSecret,
        String githubRepo,
        String ghWebhookSecret,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
