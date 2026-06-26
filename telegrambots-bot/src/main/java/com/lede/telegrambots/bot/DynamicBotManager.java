package com.lede.telegrambots.bot;

import com.lede.telegrambots.activation.ActivationResult;
import com.lede.telegrambots.activation.GroupBotActivationService;
import com.lede.telegrambots.mongo.entity.GroupActivation;
import com.lede.telegrambots.mongo.entity.ManagedBot;
import com.lede.telegrambots.shared.BotUsername;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Facade application service coordinating orchestration of database, domain logic,
 * memory caching, activations, and application events.
 */
@Service
public class DynamicBotManager implements BotManagementUseCase {

    private final BotStore botStore;
    private final BotDomainService domainService;
    private final ManagedBotRegistry registry;
    private final GroupBotActivationService activations;
    private final ApplicationEventPublisher publisher;

    public DynamicBotManager(
            BotStore botStore,
            BotDomainService domainService,
            ManagedBotRegistry registry,
            GroupBotActivationService activations,
            ApplicationEventPublisher publisher) {
        this.botStore = botStore;
        this.domainService = domainService;
        this.registry = registry;
        this.activations = activations;
        this.publisher = publisher;
    }

    @Override
    public Optional<ManagedBot> findEnabled(String username) {
        return registry.findEnabled(username);
    }

    @Override
    public Optional<ManagedBot> findAny(String username) {
        return botStore.findByUsername(BotUsername.of(username).value());
    }

    @Override
    public List<ManagedBot> listBots() {
        return botStore.findAll();
    }

    @Override
    public ManagedBot upsertBot(BotRegistration registration) {
        BotUsername username = BotUsername.of(registration.username());
        
        Optional<ManagedBot> existing = botStore.findByUsername(username.value());
        ManagedBot mergedBot = domainService.prepareForSave(existing.orElse(null), username.value(), registration);
        
        ManagedBot saved = botStore.save(mergedBot);
        
        if (saved.enabled()) {
            registry.cache(saved);
        } else {
            registry.remove(saved.username());
        }
        
        publisher.publishEvent(new BotSavedEvent(saved.username(), saved.token(), saved.tgWebhookSecret(), saved.enabled()));
        return saved;
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
        String normalized = BotUsername.of(username).value();
        Optional<ManagedBot> bot = botStore.findByUsername(normalized);
        
        activations.deleteByBotUsername(normalized);
        botStore.deleteByUsername(normalized);
        registry.remove(normalized);
        
        bot.ifPresent(b -> publisher.publishEvent(new BotDeletedEvent(b.username(), b.token())));
    }

}
