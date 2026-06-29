package com.lede.telegrambots.application.activation;

import com.lede.telegrambots.domain.activation.GroupActivation;
import com.lede.telegrambots.domain.bot.ManagedBot;

public class DeactivateContext {
    private final ManagedBot bot;
    private final long chatId;
    private GroupActivation existing;
    private boolean deactivated;

    public DeactivateContext(ManagedBot bot, long chatId) {
        this.bot = bot;
        this.chatId = chatId;
    }

    public ManagedBot getBot() { return bot; }
    public long getChatId() { return chatId; }
    public GroupActivation getExisting() { return existing; }
    public void setExisting(GroupActivation existing) { this.existing = existing; }
    public boolean isDeactivated() { return deactivated; }
    public void setDeactivated(boolean deactivated) { this.deactivated = deactivated; }
}
