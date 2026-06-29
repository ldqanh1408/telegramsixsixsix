package com.lede.telegrambots;

import com.lede.telegrambots.application.activation.GroupBotActivationService;
import com.lede.telegrambots.application.bot.BotQueryService;
import com.lede.telegrambots.application.bot.DeleteBotUseCase;
import com.lede.telegrambots.application.bot.DynamicBotManager;
import com.lede.telegrambots.application.bot.UpsertBotUseCase;
import com.lede.telegrambots.application.notification.BroadcastUseCase;
import com.lede.telegrambots.application.port.in.BotManagementUseCase;
import com.lede.telegrambots.application.port.out.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class UseCaseConfiguration {

    @Bean
    BotQueryService botQueryService(BotRepository bots) {
        return new BotQueryService(bots);
    }

    @Bean
    UpsertBotUseCase upsertBotUseCase(BotRepository bots, BotCache cache, DomainEventPublisher events) {
        return new UpsertBotUseCase(bots, cache, events);
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
