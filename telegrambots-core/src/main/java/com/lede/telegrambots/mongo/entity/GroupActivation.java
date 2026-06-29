package com.lede.telegrambots.mongo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * One activation = a binding "this bot is allowed to post in this Telegram chat".
 *
 * <p>Created when an admin in the chat issues {@code /add @<botUsername>}.</p>
 */
@Document(collection = "group_activations")
@CompoundIndexes({
        @CompoundIndex(name = "bot_chat_unique",
                def = "{'botUsername': 1, 'chatId': 1}",
                unique = true)
})
public record GroupActivation(
        @Id String id,
        @Indexed String botId,
        @Indexed String botUsername,
        long chatId,
        boolean active,
        Instant activatedAt,
        Instant updatedAt
) {
}
