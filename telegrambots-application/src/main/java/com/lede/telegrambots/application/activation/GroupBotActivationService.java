package com.lede.telegrambots.application.activation;

import com.lede.telegrambots.domain.pipeline.Pipeline;
import com.lede.telegrambots.application.port.out.ActivationRepository;
import com.lede.telegrambots.application.activation.steps.*;
import com.lede.telegrambots.domain.activation.ActivationResult;
import com.lede.telegrambots.domain.activation.GroupActivation;
import com.lede.telegrambots.domain.bot.ManagedBot;
import com.lede.telegrambots.domain.shared.BotUsername;

import java.util.List;

public class GroupBotActivationService {

    private final ActivationRepository store;
    private final Pipeline<ActivateContext, ActivationResult> activatePipeline;
    private final Pipeline<DeactivateContext, Boolean> deactivatePipeline;

    // Normal constructor
    public GroupBotActivationService(ActivationRepository store) {
        this.store = store;
        this.activatePipeline = new Pipeline<>(List.of(
                new LoadExistingActivationForActivateStep(store),
                new CheckAlreadyActiveStep(),
                new SaveActivationStep(store)
        ));
        this.deactivatePipeline = new Pipeline<>(List.of(
                new LoadExistingActivationForDeactivateStep(store),
                new CheckNotActiveStep(),
                new SaveDeactivatedStep(store)
        ));
    }

    // Spring autowired constructor
    public GroupBotActivationService(
            ActivationRepository store,
            List<ActivateGroupStep> activateSteps,
            List<DeactivateGroupStep> deactivateSteps) {
        this.store = store;
        this.activatePipeline = new Pipeline<>(activateSteps);
        this.deactivatePipeline = new Pipeline<>(deactivateSteps);
    }

    public ActivationResult activate(ManagedBot bot, long chatId) {
        return activatePipeline.run(new ActivateContext(bot, chatId))
                .orElseThrow(() -> new IllegalStateException("activation pipeline failed to yield a result"));
    }

    public GroupActivationCommandResult activateRequested(ManagedBot bot, long chatId, String requestedUsername) {
        if (requestedUsernameMissing(requestedUsername)) {
            return GroupActivationCommandResult.missingUsername();
        }
        if (targetsDifferentBot(bot, requestedUsername)) {
            return GroupActivationCommandResult.botMismatch();
        }

        ActivationResult result = activate(bot, chatId);
        return result.newlyActivated()
                ? GroupActivationCommandResult.activated(result)
                : GroupActivationCommandResult.alreadyActive(result);
    }

    public boolean deactivate(ManagedBot bot, long chatId) {
        return deactivatePipeline.run(new DeactivateContext(bot, chatId))
                .orElse(false);
    }

    public GroupActivationCommandResult deactivateRequested(ManagedBot bot, long chatId, String requestedUsername) {
        if (requestedUsernameMissing(requestedUsername)) {
            return GroupActivationCommandResult.missingUsername();
        }
        if (targetsDifferentBot(bot, requestedUsername)) {
            return GroupActivationCommandResult.botMismatch();
        }

        return deactivate(bot, chatId)
                ? GroupActivationCommandResult.deactivated()
                : GroupActivationCommandResult.notActive();
    }

    public boolean isActive(ManagedBot bot, long chatId) {
        return store.isActive(bot.username(), chatId);
    }

    public long activeGroupCount(ManagedBot bot) {
        return store.countActive(bot.username());
    }

    public List<GroupActivation> activeGroups(ManagedBot bot) {
        return store.findActive(bot.username());
    }

    public void deleteByBotUsername(String username) {
        store.deleteAllFor(BotUsername.of(username).value());
    }

    private static boolean requestedUsernameMissing(String requestedUsername) {
        return BotUsername.of(requestedUsername).isBlank();
    }

    private static boolean targetsDifferentBot(ManagedBot bot, String requestedUsername) {
        return !BotUsername.of(requestedUsername).value().equals(BotUsername.of(bot.username()).value());
    }
}
