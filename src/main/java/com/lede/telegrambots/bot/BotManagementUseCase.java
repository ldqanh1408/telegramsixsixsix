package com.lede.telegrambots.bot;

import com.lede.telegrambots.activation.ActivationResult;
import com.lede.telegrambots.mongo.entity.GroupActivation;
import com.lede.telegrambots.mongo.entity.ManagedBot;

import java.util.List;
import java.util.Optional;

public interface BotManagementUseCase {

    Optional<ManagedBot> findEnabled(String username);

    Optional<ManagedBot> findAny(String username);

    List<ManagedBot> listBots();

    ManagedBot upsertBot(BotRegistration registration);

    ActivationResult activate(ManagedBot bot, long chatId);

    boolean deactivate(ManagedBot bot, long chatId);

    boolean isActive(ManagedBot bot, long chatId);

    long activeGroupCount(ManagedBot bot);

    List<GroupActivation> activeGroups(ManagedBot bot);

    void deleteBot(String username);
}
