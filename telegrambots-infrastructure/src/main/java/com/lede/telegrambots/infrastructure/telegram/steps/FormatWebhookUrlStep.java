package com.lede.telegrambots.infrastructure.telegram.steps;

import com.lede.telegrambots.domain.pipeline.Step;
import com.lede.telegrambots.infrastructure.telegram.SyncSavedBotContext;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

@Component
@Order(3)
public class FormatWebhookUrlStep implements Step<SyncSavedBotContext, Boolean> {
    @Override
    public Optional<Boolean> execute(SyncSavedBotContext ctx) {
        String webhookUrl = ctx.getPublicUrl();
        if (!webhookUrl.endsWith("/")) {
            webhookUrl += "/";
        }
        webhookUrl += "telegram/webhook/" + ctx.getEvent().username();
        ctx.setWebhookUrl(webhookUrl);
        return Optional.empty();
    }
}
