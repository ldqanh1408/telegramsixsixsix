package com.lede.telegrambots.infrastructure.event;

import com.lede.telegrambots.application.port.out.DomainEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Infrastructure adapter for the {@link DomainEventPublisher} port. Forwards application/domain
 * events to Spring's {@link ApplicationEventPublisher}, which drives the {@code @EventListener}s
 * (e.g. {@code TelegramWebhookManager}).
 */
@Component
class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher delegate;

    SpringDomainEventPublisher(ApplicationEventPublisher delegate) {
        this.delegate = delegate;
    }

    @Override
    public void publish(Object event) {
        delegate.publishEvent(event);
    }
}
