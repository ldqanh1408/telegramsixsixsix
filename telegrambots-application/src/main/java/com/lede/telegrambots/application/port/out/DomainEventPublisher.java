package com.lede.telegrambots.application.port.out;

/**
 * Outbound port for publishing domain events (e.g. {@code BotSavedEvent}, {@code BotDeletedEvent}).
 *
 * <p>Use cases publish through this abstraction so the application stays free of the concrete
 * eventing mechanism. The infrastructure adapter forwards to Spring's {@code ApplicationEventPublisher},
 * which is what drives the {@code @EventListener}s in the web layer.</p>
 */
public interface DomainEventPublisher {

    void publish(Object event);
}
