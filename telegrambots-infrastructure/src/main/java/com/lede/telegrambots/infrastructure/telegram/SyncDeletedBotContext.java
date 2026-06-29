package com.lede.telegrambots.infrastructure.telegram;

import com.lede.telegrambots.domain.bot.BotDeletedEvent;

public class SyncDeletedBotContext {
    private final BotDeletedEvent event;

    public SyncDeletedBotContext(BotDeletedEvent event) {
        this.event = event;
    }

    public BotDeletedEvent getEvent() { return event; }
}
