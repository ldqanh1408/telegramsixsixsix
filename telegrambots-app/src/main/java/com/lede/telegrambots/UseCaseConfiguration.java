package com.lede.telegrambots;

import com.lede.telegrambots.application.activation.ActivateGroupStep;
import com.lede.telegrambots.application.activation.DeactivateGroupStep;
import com.lede.telegrambots.application.activation.GroupBotActivationService;
import com.lede.telegrambots.application.activation.steps.*;
import com.lede.telegrambots.application.bot.BotDeleteStep;
import com.lede.telegrambots.application.bot.BotQueryService;
import com.lede.telegrambots.application.bot.BotUpsertStep;
import com.lede.telegrambots.application.bot.DeleteBotUseCase;
import com.lede.telegrambots.application.bot.DynamicBotManager;
import com.lede.telegrambots.application.bot.UpsertBotUseCase;
import com.lede.telegrambots.application.notification.BroadcastStep;
import com.lede.telegrambots.application.bot.steps.*;
import com.lede.telegrambots.application.notification.BroadcastUseCase;
import com.lede.telegrambots.application.notification.steps.*;
import com.lede.telegrambots.application.port.in.BotManagementUseCase;
import com.lede.telegrambots.application.port.out.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;

@Configuration
class UseCaseConfiguration {

    @Bean
    BotQueryService botQueryService(BotRepository bots) {
        return new BotQueryService(bots);
    }

    // --- UpsertBot Steps ---
    @Bean
    @Order(1)
    NormalizeAndLoadStep normalizeAndLoadStep(BotRepository bots) {
        return new NormalizeAndLoadStep(bots);
    }

    @Bean
    @Order(2)
    MergeStep mergeStep() {
        return new MergeStep();
    }

    @Bean
    @Order(3)
    PersistStep persistStep(BotRepository bots) {
        return new PersistStep(bots);
    }

    @Bean
    @Order(4)
    RefreshCacheStep refreshCacheStep(BotCache cache) {
        return new RefreshCacheStep(cache);
    }

    @Bean
    @Order(5)
    PublishEventStep publishEventStep(DomainEventPublisher events) {
        return new PublishEventStep(events);
    }

    @Bean
    UpsertBotUseCase upsertBotUseCase(List<BotUpsertStep> steps) {
        return new UpsertBotUseCase(steps);
    }

    // --- DeleteBot Steps ---
    @Bean
    @Order(1)
    LoadStep loadStep(BotRepository bots) {
        return new LoadStep(bots);
    }

    @Bean
    @Order(2)
    DeleteActivationsStep deleteActivationsStep(ActivationRepository activations) {
        return new DeleteActivationsStep(activations);
    }

    @Bean
    @Order(3)
    DeleteBotStep deleteBotStep(BotRepository bots) {
        return new DeleteBotStep(bots);
    }

    @Bean
    @Order(4)
    EvictCacheStep evictCacheStep(BotCache cache) {
        return new EvictCacheStep(cache);
    }

    @Bean
    @Order(5)
    PublishDeleteEventStep publishDeleteEventStep(DomainEventPublisher events) {
        return new PublishDeleteEventStep(events);
    }

    @Bean
    DeleteBotUseCase deleteBotUseCase(List<BotDeleteStep> steps) {
        return new DeleteBotUseCase(steps);
    }

    // --- Activate Steps ---
    @Bean
    @Order(1)
    LoadExistingActivationForActivateStep loadExistingActivationForActivateStep(ActivationRepository store) {
        return new LoadExistingActivationForActivateStep(store);
    }

    @Bean
    @Order(2)
    CheckAlreadyActiveStep checkAlreadyActiveStep() {
        return new CheckAlreadyActiveStep();
    }

    @Bean
    @Order(3)
    SaveActivationStep saveActivationStep(ActivationRepository store) {
        return new SaveActivationStep(store);
    }

    // --- Deactivate Steps ---
    @Bean
    @Order(1)
    LoadExistingActivationForDeactivateStep loadExistingActivationForDeactivateStep(ActivationRepository store) {
        return new LoadExistingActivationForDeactivateStep(store);
    }

    @Bean
    @Order(2)
    CheckNotActiveStep checkNotActiveStep() {
        return new CheckNotActiveStep();
    }

    @Bean
    @Order(3)
    SaveDeactivatedStep saveDeactivatedStep(ActivationRepository store) {
        return new SaveDeactivatedStep(store);
    }

    @Bean
    GroupBotActivationService groupBotActivationService(
            ActivationRepository activations,
            List<ActivateGroupStep> activateSteps,
            List<DeactivateGroupStep> deactivateSteps) {
        return new GroupBotActivationService(activations, activateSteps, deactivateSteps);
    }

    // --- Broadcast Steps ---
    @Bean
    @Order(1)
    LoadTargetsStep loadTargetsStep(ActivationRepository activations) {
        return new LoadTargetsStep(activations);
    }

    @Bean
    @Order(2)
    ValidateTargetsStep validateTargetsStep() {
        return new ValidateTargetsStep();
    }

    @Bean
    @Order(3)
    DeliverMessagesStep deliverMessagesStep(TelegramGateway telegram) {
        return new DeliverMessagesStep(telegram);
    }

    @Bean
    BroadcastUseCase broadcastUseCase(List<BroadcastStep> steps) {
        return new BroadcastUseCase(steps);
    }

    @Bean
    BotManagementUseCase botManagementUseCase(BotCache cache,
                                              BotQueryService queries,
                                              UpsertBotUseCase upsertBot,
                                              DeleteBotUseCase deleteBot,
                                              GroupBotActivationService activations) {
        return new DynamicBotManager(cache, queries, upsertBot, deleteBot, activations);
    }
}
