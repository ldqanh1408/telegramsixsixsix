package com.lede.telegrambots.github;

import com.lede.telegrambots.bot.BotManagementUseCase;
import com.lede.telegrambots.github.GitHubWebhookResult.Outcome;
import com.lede.telegrambots.mongo.entity.ManagedBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(1)
class BotLookupStep implements GitHubWebhookStep {

    private static final Logger log = LoggerFactory.getLogger(BotLookupStep.class);
    private final BotManagementUseCase bots;

    public BotLookupStep(BotManagementUseCase bots) {
        this.bots = bots;
    }

    @Override
    public Optional<GitHubWebhookResult> execute(GitHubWebhookContext context) {
        Optional<ManagedBot> maybe = bots.findEnabled(context.getBotUsername());
        if (maybe.isEmpty()) {
            log.warn("GitHub webhook delivery={} for unknown/disabled bot @{}",
                    context.getDelivery(), context.getBotUsername());
            return Optional.of(GitHubWebhookResult.of(Outcome.UNKNOWN_BOT, "unknown bot"));
        }
        context.setBot(maybe.get());
        return Optional.empty();
    }
}
