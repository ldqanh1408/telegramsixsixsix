package com.lede.telegrambots.infrastructure.telegram;

import com.lede.telegrambots.domain.bot.BotSavedEvent;

public class SyncSavedBotContext {
    private final BotSavedEvent event;
    private final String publicUrl;
    private String webhookUrl;
    private boolean success;

    public SyncSavedBotContext(BotSavedEvent event, String publicUrl) {
        this.event = event;
        this.publicUrl = publicUrl;
    }

    public BotSavedEvent getEvent() { return event; }
    public String getPublicUrl() { return publicUrl; }
    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}
