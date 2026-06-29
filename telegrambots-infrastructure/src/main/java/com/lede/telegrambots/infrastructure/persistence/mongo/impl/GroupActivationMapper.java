package com.lede.telegrambots.infrastructure.persistence.mongo.impl;

import com.lede.telegrambots.domain.activation.GroupActivation;

/** Translates between the pure domain {@link GroupActivation} and its Mongo {@link GroupActivationDocument}. */
final public class GroupActivationMapper {

    private GroupActivationMapper() {}

    public static GroupActivation toDomain(GroupActivationDocument doc) {
        return new GroupActivation(
                doc.id(),
                doc.botId(),
                doc.botUsername(),
                doc.chatId(),
                doc.active(),
                doc.activatedAt(),
                doc.updatedAt()
        );
    }

    public static GroupActivationDocument toDocument(GroupActivation activation) {
        return new GroupActivationDocument(
                activation.id(),
                activation.botId(),
                activation.botUsername(),
                activation.chatId(),
                activation.active(),
                activation.activatedAt(),
                activation.updatedAt()
        );
    }
}
