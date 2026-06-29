package com.lede.telegrambots.application.bot;

import com.lede.telegrambots.application.pipeline.Pipeline;
import com.lede.telegrambots.application.pipeline.Step;
import com.lede.telegrambots.application.port.out.ActivationRepository;
import com.lede.telegrambots.application.port.out.BotCache;
import com.lede.telegrambots.application.port.out.BotRepository;
import com.lede.telegrambots.application.port.out.DomainEventPublisher;
import com.lede.telegrambots.domain.bot.BotDeletedEvent;
import com.lede.telegrambots.domain.bot.ManagedBot;
import com.lede.telegrambots.domain.shared.BotUsername;

import java.util.List;
import java.util.Optional;

/**
 * Delete a managed bot and everything bound to it, expressed as a {@link Pipeline}:
 *
 * <pre>load → delete activations → delete bot → evict cache → publish event (if it existed)</pre>
 *
 * All stages run; the final stage returns {@code true} as the pipeline result.
 */
public class DeleteBotUseCase {

    private final Pipeline<Context, Boolean> pipeline;

    public DeleteBotUseCase(BotRepository bots,
                            ActivationRepository activations,
                            BotCache cache,
                            DomainEventPublisher events) {
        this.pipeline = new Pipeline<>(List.of(
                new LoadStep(bots),
                new DeleteActivationsStep(activations),
                new DeleteBotStep(bots),
                new EvictCacheStep(cache),
                new PublishEventStep(events)
        ));
    }

    public void delete(String username) {
        pipeline.run(new Context(BotUsername.of(username).value()));
    }

    /** Mutable state threaded through the delete stages. */
    static final class Context {
        final String username;
        ManagedBot existing;

        Context(String username) {
            this.username = username;
        }
    }

    private record LoadStep(BotRepository bots) implements Step<Context, Boolean> {
        @Override
        public Optional<Boolean> execute(Context ctx) {
            ctx.existing = bots.findByUsername(ctx.username).orElse(null);
            return Optional.empty();
        }
    }

    private record DeleteActivationsStep(ActivationRepository activations) implements Step<Context, Boolean> {
        @Override
        public Optional<Boolean> execute(Context ctx) {
            activations.deleteAllFor(ctx.username);
            return Optional.empty();
        }
    }

    private record DeleteBotStep(BotRepository bots) implements Step<Context, Boolean> {
        @Override
        public Optional<Boolean> execute(Context ctx) {
            bots.deleteByUsername(ctx.username);
            return Optional.empty();
        }
    }

    private record EvictCacheStep(BotCache cache) implements Step<Context, Boolean> {
        @Override
        public Optional<Boolean> execute(Context ctx) {
            cache.evict(ctx.username);
            return Optional.empty();
        }
    }

    private record PublishEventStep(DomainEventPublisher events) implements Step<Context, Boolean> {
        @Override
        public Optional<Boolean> execute(Context ctx) {
            if (ctx.existing != null) {
                events.publish(new BotDeletedEvent(ctx.existing.username(), ctx.existing.token()));
            }
            return Optional.of(Boolean.TRUE);
        }
    }
}
