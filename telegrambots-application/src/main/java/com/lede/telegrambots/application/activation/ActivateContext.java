package com.lede.telegrambots.application.activation;

import com.lede.telegrambots.domain.activation.ActivationResult;
import com.lede.telegrambots.domain.activation.GroupActivation;
import com.lede.telegrambots.domain.bot.ManagedBot;

public class ActivateContext {
    private final ManagedBot bot;
    private final long chatId;
    private GroupActivation existing;
    private ActivationResult result;

    public ActivateContext(ManagedBot bot, long chatId) {
        this.bot = bot;
        this.chatId = chatId;
    }

    public ManagedBot getBot() { return bot; }
    public long getChatId() { return chatId; }
    public GroupActivation getExisting() { return existing; }
    public void setExisting(GroupActivation existing) { this.existing = existing; }
    public ActivationResult getResult() { return result; }
    public void setResult(ActivationResult result) { this.result = result; }
}
