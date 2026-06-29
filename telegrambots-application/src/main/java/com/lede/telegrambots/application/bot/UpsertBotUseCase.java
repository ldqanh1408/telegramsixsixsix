package com.lede.telegrambots.application.bot;

import com.lede.telegrambots.application.pipeline.Pipeline;
import com.lede.telegrambots.application.port.out.BotCache;
import com.lede.telegrambots.application.port.out.BotRepository;
import com.lede.telegrambots.application.port.out.DomainEventPublisher;
import com.lede.telegrambots.application.bot.steps.*;
import com.lede.telegrambots.domain.bot.BotDomainService;
import com.lede.telegrambots.domain.bot.BotRegistration;
import com.lede.telegrambots.domain.bot.ManagedBot;
import java.util.List;

public class UpsertBotUseCase {

    private final Pipeline<UpsertBotContext, ManagedBot> pipeline;

    public UpsertBotUseCase(BotRepository bots,
                            BotDomainService domainService,
                            BotCache cache,
                            DomainEventPublisher events) {
        this.pipeline = new Pipeline<>(List.of(
                new NormalizeAndLoadStep(bots),
                new MergeStep(domainService),
                new PersistStep(bots),
                new RefreshCacheStep(cache),
                new PublishEventStep(events)
        ));
    }

    public ManagedBot upsert(BotRegistration registration) {
        return pipeline.run(new UpsertBotContext(registration))
                .orElseThrow(() -> new IllegalStateException("upsert pipeline produced no result"));
    }
}
