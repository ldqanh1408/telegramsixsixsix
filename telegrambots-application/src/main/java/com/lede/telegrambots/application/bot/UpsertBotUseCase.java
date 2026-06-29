package com.lede.telegrambots.application.bot;

import com.lede.telegrambots.application.pipeline.Pipeline;
import com.lede.telegrambots.application.pipeline.Step;
import com.lede.telegrambots.application.port.out.BotCache;
import com.lede.telegrambots.application.port.out.BotRepository;
import com.lede.telegrambots.application.port.out.DomainEventPublisher;
import com.lede.telegrambots.domain.bot.BotDomainService;
import com.lede.telegrambots.domain.bot.BotRegistration;
import com.lede.telegrambots.domain.bot.BotSavedEvent;
import com.lede.telegrambots.domain.bot.ManagedBot;
import com.lede.telegrambots.domain.shared.BotUsername;

import java.util.List;
import java.util.Optional;

/**
 * Create-or-update a managed bot, expressed as a {@link Pipeline} of explicit business stages:
 *
 * <pre>normalize+load → merge (domain rules) → persist → refresh cache → publish event</pre>
 *
 * Each stage is a named {@link Step}, so the write flow reads top-to-bottom and new stages slot in
 * without touching the others. All stages proceed; the final stage emits the saved bot as the
 * pipeline result.
 */
public class UpsertBotUseCase {

    private final Pipeline<Context, ManagedBot> pipeline;

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
        return pipeline.run(new Context(registration))
                .orElseThrow(() -> new IllegalStateException("upsert pipeline produced no result"));
    }

    /** Mutable state threaded through the upsert stages. */
    static final class Context {
        final BotRegistration registration;
        String username;
        ManagedBot existing;
        ManagedBot toSave;
        ManagedBot saved;

        Context(BotRegistration registration) {
            this.registration = registration;
        }
    }

    private record NormalizeAndLoadStep(BotRepository bots) implements Step<Context, ManagedBot> {
        @Override
        public Optional<ManagedBot> execute(Context ctx) {
            ctx.username = BotUsername.of(ctx.registration.username()).value();
            ctx.existing = bots.findByUsername(ctx.username).orElse(null);
            return Optional.empty();
        }
    }

    private record MergeStep(BotDomainService domainService) implements Step<Context, ManagedBot> {
        @Override
        public Optional<ManagedBot> execute(Context ctx) {
            ctx.toSave = domainService.prepareForSave(ctx.existing, ctx.username, ctx.registration);
            return Optional.empty();
        }
    }

    private record PersistStep(BotRepository bots) implements Step<Context, ManagedBot> {
        @Override
        public Optional<ManagedBot> execute(Context ctx) {
            ctx.saved = bots.save(ctx.toSave);
            return Optional.empty();
        }
    }

    private record RefreshCacheStep(BotCache cache) implements Step<Context, ManagedBot> {
        @Override
        public Optional<ManagedBot> execute(Context ctx) {
            if (ctx.saved.enabled()) {
                cache.put(ctx.saved);
            } else {
                cache.evict(ctx.saved.username());
            }
            return Optional.empty();
        }
    }

    private record PublishEventStep(DomainEventPublisher events) implements Step<Context, ManagedBot> {
        @Override
        public Optional<ManagedBot> execute(Context ctx) {
            ManagedBot saved = ctx.saved;
            events.publish(new BotSavedEvent(saved.username(), saved.token(), saved.tgWebhookSecret(), saved.enabled()));
            return Optional.of(saved);
        }
    }
}
