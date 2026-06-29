package com.lede.telegrambots.application.bot;

import com.lede.telegrambots.domain.bot.ManagedBot;

public class DeleteBotContext {
    private final String username;
    private ManagedBot existing;

    public DeleteBotContext(String username) {
        this.username = username;
    }

    public String getUsername() { return username; }
    public ManagedBot getExisting() { return existing; }
    public void setExisting(ManagedBot existing) { this.existing = existing; }
}
