package com.lede.telegrambots.application.bot;

import com.lede.telegrambots.domain.bot.BotRegistration;
import com.lede.telegrambots.domain.bot.ManagedBot;

public class UpsertBotContext {
    private final BotRegistration registration;
    private String username;
    private ManagedBot existing;
    private ManagedBot toSave;
    private ManagedBot saved;

    public UpsertBotContext(BotRegistration registration) {
        this.registration = registration;
    }

    public BotRegistration getRegistration() { return registration; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public ManagedBot getExisting() { return existing; }
    public void setExisting(ManagedBot existing) { this.existing = existing; }
    public ManagedBot getToSave() { return toSave; }
    public void setToSave(ManagedBot toSave) { this.toSave = toSave; }
    public ManagedBot getSaved() { return saved; }
    public void setSaved(ManagedBot saved) { this.saved = saved; }
}
