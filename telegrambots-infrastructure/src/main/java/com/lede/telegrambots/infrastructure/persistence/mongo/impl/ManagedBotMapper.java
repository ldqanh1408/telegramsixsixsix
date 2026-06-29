package com.lede.telegrambots.infrastructure.persistence.mongo.impl;

import com.lede.telegrambots.domain.bot.ManagedBot;

/** Translates between the pure domain {@link ManagedBot} and its Mongo {@link ManagedBotDocument}. */
final public class ManagedBotMapper {

    private ManagedBotMapper() {}

    public static ManagedBot toDomain(ManagedBotDocument doc) {
        return new ManagedBot(
                doc.id(),
                doc.username(),
                doc.token(),
                doc.tgWebhookSecret(),
                doc.githubRepo(),
                doc.ghWebhookSecret(),
                doc.enabled(),
                doc.createdAt(),
                doc.updatedAt()
        );
    }

    public static ManagedBotDocument toDocument(ManagedBot bot) {
        return new ManagedBotDocument(
                bot.id(),
                bot.username(),
                bot.token(),
                bot.tgWebhookSecret(),
                bot.githubRepo(),
                bot.ghWebhookSecret(),
                bot.enabled(),
                bot.createdAt(),
                bot.updatedAt()
        );
    }
}
