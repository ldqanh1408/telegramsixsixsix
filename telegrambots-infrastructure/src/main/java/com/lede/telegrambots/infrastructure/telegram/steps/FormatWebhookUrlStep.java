package com.lede.telegrambots.infrastructure.telegram.steps;

import com.lede.telegrambots.application.pipeline.Step;
import com.lede.telegrambots.infrastructure.telegram.SyncSavedBotContext;
import java.util.Optional;

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
