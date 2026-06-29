package com.lede.telegrambots.application.port.in;

import com.lede.telegrambots.domain.activation.ActivationResult;
import com.lede.telegrambots.domain.activation.GroupActivation;
import com.lede.telegrambots.application.activation.GroupActivationCommandResult;
import com.lede.telegrambots.domain.bot.BotRegistration;
import com.lede.telegrambots.domain.bot.ManagedBot;

import java.util.List;
import java.util.Optional;

/**
 * Inbound (driving) port: the single facade the web layer talks to for all bot management and
 * activation operations. Implemented by {@code DynamicBotManager}, which delegates to the
 * focused use cases behind it.
 */
public interface BotManagementUseCase {

    Optional<ManagedBot> findEnabled(String username);

    Optional<ManagedBot> findAny(String username);

    List<ManagedBot> listBots();

    ManagedBot upsertBot(BotRegistration registration);

    ActivationResult activate(ManagedBot bot, long chatId);

    GroupActivationCommandResult activateRequested(ManagedBot bot, long chatId, String requestedUsername);

    boolean deactivate(ManagedBot bot, long chatId);

    GroupActivationCommandResult deactivateRequested(ManagedBot bot, long chatId, String requestedUsername);

    boolean isActive(ManagedBot bot, long chatId);

    long activeGroupCount(ManagedBot bot);

    List<GroupActivation> activeGroups(ManagedBot bot);

    void deleteBot(String username);
}
