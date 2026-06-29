package com.lede.telegrambots.application.bot;

import com.lede.telegrambots.application.pipeline.Pipeline;
import com.lede.telegrambots.application.port.out.ActivationRepository;
import com.lede.telegrambots.application.port.out.BotCache;
import com.lede.telegrambots.application.port.out.BotRepository;
import com.lede.telegrambots.application.port.out.DomainEventPublisher;
import com.lede.telegrambots.application.bot.steps.*;
import java.util.List;

public class DeleteBotUseCase {

    private final Pipeline<DeleteBotContext, Boolean> pipeline;

    public DeleteBotUseCase(BotRepository bots,
                            ActivationRepository activations,
                            BotCache cache,
                            DomainEventPublisher events) {
        this.pipeline = new Pipeline<>(List.of(
                new LoadStep(bots),
                new DeleteActivationsStep(activations),
                new DeleteBotStep(bots),
                new EvictCacheStep(cache),
                new PublishDeleteEventStep(events)
        ));
    }

    public void delete(String username) {
        pipeline.run(new DeleteBotContext(username));
    }
}
