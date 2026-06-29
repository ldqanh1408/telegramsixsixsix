package com.lede.telegrambots;

import com.lede.telegrambots.application.activation.GroupBotActivationService;
import com.lede.telegrambots.application.bot.BotQueryService;
import com.lede.telegrambots.application.bot.DeleteBotUseCase;
import com.lede.telegrambots.application.bot.DynamicBotManager;
import com.lede.telegrambots.application.bot.UpsertBotUseCase;
import com.lede.telegrambots.application.notification.BroadcastUseCase;
import com.lede.telegrambots.application.port.in.BotManagementUseCase;
import com.lede.telegrambots.application.port.out.ActivationRepository;
import com.lede.telegrambots.application.port.out.BotCache;
import com.lede.telegrambots.application.port.out.BotRepository;
import com.lede.telegrambots.application.port.out.DomainEventPublisher;
import com.lede.telegrambots.application.port.out.TelegramGateway;
import com.lede.telegrambots.domain.bot.BotDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition root for the (framework-free) application layer. The use cases are plain Java objects;
 * here — in the only module that depends on every layer — they are wired into Spring beans against
 * the outbound ports, whose implementations live in the infrastructure layer and are component-scanned.
 */
@Configuration
class UseCaseConfiguration {

    @Bean
    BotDomainService botDomainService() {
        return new BotDomainService();
    }

    @Bean
    BotQueryService botQueryService(BotRepository bots) {
        return new BotQueryService(bots);
    }

    @Bean
    UpsertBotUseCase upsertBotUseCase(BotRepository bots,
                                      BotDomainService domainService,
                                      BotCache cache,
                                      DomainEventPublisher events) {
        return new UpsertBotUseCase(bots, domainService, cache, events);
    }

    @Bean
    DeleteBotUseCase deleteBotUseCase(BotRepository bots,
                                      ActivationRepository activations,
                                      BotCache cache,
                                      DomainEventPublisher events) {
        return new DeleteBotUseCase(bots, activations, cache, events);
    }

    @Bean
    GroupBotActivationService groupBotActivationService(ActivationRepository activations) {
        return new GroupBotActivationService(activations);
    }

    @Bean
    BroadcastUseCase broadcastUseCase(TelegramGateway telegram, ActivationRepository activations) {
        return new BroadcastUseCase(telegram, activations);
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
