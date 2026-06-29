package com.lede.telegrambots.infrastructure.telegram.steps;

import com.lede.telegrambots.application.pipeline.Step;
import com.lede.telegrambots.infrastructure.telegram.SyncSavedBotContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;

public class CheckPublicUrlStep implements Step<SyncSavedBotContext, Boolean> {
    private static final Logger log = LoggerFactory.getLogger(CheckPublicUrlStep.class);

    @Override
    public Optional<Boolean> execute(SyncSavedBotContext ctx) {
        if (ctx.getPublicUrl() == null || ctx.getPublicUrl().isEmpty()) {
            log.warn("PUBLIC_URL is not configured; automatic Telegram setWebhook for @{} is skipped. " +
                    "Please configure app.public-url in application.yaml or set PUBLIC_URL environment variable.",
                    ctx.getEvent().username());
            return Optional.of(Boolean.FALSE);
        }
        return Optional.empty();
    }
}
