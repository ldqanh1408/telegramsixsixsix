package com.lede.telegrambots.application.notification;

import com.lede.telegrambots.domain.activation.GroupActivation;
import com.lede.telegrambots.domain.bot.ManagedBot;
import java.util.List;

public class BroadcastContext {
    private final ManagedBot bot;
    private final String html;
    private List<GroupActivation> targets;

    public BroadcastContext(ManagedBot bot, String html) {
        this.bot = bot;
        this.html = html;
    }

    public ManagedBot getBot() { return bot; }
    public String getHtml() { return html; }
    public List<GroupActivation> getTargets() { return targets; }
    public void setTargets(List<GroupActivation> targets) { this.targets = targets; }
}
