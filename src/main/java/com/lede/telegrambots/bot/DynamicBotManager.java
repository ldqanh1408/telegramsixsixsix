package com.lede.telegrambots.bot;

import com.lede.telegrambots.activation.ActivationResult;
import com.lede.telegrambots.activation.GroupBotActivationService;
import com.lede.telegrambots.mongo.entity.GroupActivation;
import com.lede.telegrambots.mongo.entity.ManagedBot;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DynamicBotManager implements BotManagementUseCase {

    private final ManagedBotRegistry registry;
    private final GroupBotActivationService activations;

    public DynamicBotManager(ManagedBotRegistry registry, GroupBotActivationService activations) {
        this.registry = registry;
        this.activations = activations;
    }

    @Override
    public Optional<ManagedBot> findEnabled(String username) {
        return registry.findEnabled(username);
    }

    @Override
    public Optional<ManagedBot> findAny(String username) {
        return registry.findAny(username);
    }

    @Override
    public List<ManagedBot> listBots() {
        return registry.listBots();
    }

    @Override
    public ManagedBot upsertBot(BotRegistration registration) {
        return registry.upsert(registration);
    }

    @Override
    public ActivationResult activate(ManagedBot bot, long chatId) {
        return activations.activate(bot, chatId);
    }

    @Override
    public boolean deactivate(ManagedBot bot, long chatId) {
        return activations.deactivate(bot, chatId);
    }

    @Override
    public boolean isActive(ManagedBot bot, long chatId) {
        return activations.isActive(bot, chatId);
    }

    @Override
    public long activeGroupCount(ManagedBot bot) {
        return activations.activeGroupCount(bot);
    }

    @Override
    public List<GroupActivation> activeGroups(ManagedBot bot) {
        return activations.activeGroups(bot);
    }

    @Override
    public void deleteBot(String username) {
        activations.deleteByBotUsername(username);
        registry.delete(username);
    }

}
