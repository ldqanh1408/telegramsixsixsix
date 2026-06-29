package com.lede.telegrambots.application.bot;

import com.lede.telegrambots.domain.pipeline.Pipeline;
import com.lede.telegrambots.application.port.out.ActivationRepository;
import com.lede.telegrambots.application.port.out.BotCache;
import com.lede.telegrambots.application.port.out.BotRepository;
import com.lede.telegrambots.application.port.out.DomainEventPublisher;
import com.lede.telegrambots.application.bot.steps.*;
import java.util.List;

public class DeleteBotUseCase {

    private final Pipeline<DeleteBotContext, Boolean> pipeline;

    // Normal constructor
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

    // Spring autowired constructor
    public DeleteBotUseCase(List<BotDeleteStep> steps) {
        this.pipeline = new Pipeline<>(steps);
    }

    public void delete(String username) {
        pipeline.run(new DeleteBotContext(username));
    }
}
