package com.lede.telegrambots.application.bot;

import com.lede.telegrambots.application.activation.GroupBotActivationService;
import com.lede.telegrambots.application.port.in.BotManagementUseCase;
import com.lede.telegrambots.application.port.out.BotCache;
import com.lede.telegrambots.domain.activation.ActivationResult;
import com.lede.telegrambots.domain.activation.GroupActivation;
import com.lede.telegrambots.domain.bot.BotRegistration;
import com.lede.telegrambots.domain.bot.ManagedBot;

import java.util.List;
import java.util.Optional;

/**
 * Facade implementing the inbound {@link BotManagementUseCase} port. It owns no business rules
 * itself — it simply routes each call to the focused use case responsible for it (queries, upsert,
 * delete, activation), giving the web layer one cohesive entry point.
 */
public class DynamicBotManager implements BotManagementUseCase {

    private final BotCache cache;
    private final BotQueryService queries;
    private final UpsertBotUseCase upsertBot;
    private final DeleteBotUseCase deleteBot;
    private final GroupBotActivationService activations;

    public DynamicBotManager(BotCache cache,
                             BotQueryService queries,
                             UpsertBotUseCase upsertBot,
                             DeleteBotUseCase deleteBot,
                             GroupBotActivationService activations) {
        this.cache = cache;
        this.queries = queries;
        this.upsertBot = upsertBot;
        this.deleteBot = deleteBot;
        this.activations = activations;
    }

    @Override
    public Optional<ManagedBot> findEnabled(String username) {
        return cache.findEnabled(username);
    }

    @Override
    public Optional<ManagedBot> findAny(String username) {
        return queries.findAny(username);
    }

    @Override
    public List<ManagedBot> listBots() {
        return queries.listBots();
    }

    @Override
    public ManagedBot upsertBot(BotRegistration registration) {
        return upsertBot.upsert(registration);
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
        deleteBot.delete(username);
    }
}
