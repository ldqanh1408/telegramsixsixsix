package com.lede.telegrambots.telegram.command;

import com.lede.telegrambots.telegram.MessageFormatter;
import com.lede.telegrambots.telegram.TelegramClient;
import org.springframework.stereotype.Component;

@Component
class IdCommand implements BotCommand {

    private final TelegramClient telegram;

    IdCommand(TelegramClient telegram) {
        this.telegram = telegram;
    }

    @Override public String name() { return "/id"; }

    @Override
    public void execute(CommandContext ctx) {
        telegram.sendHtml(ctx.bot().token(), ctx.chatId(),
                "Chat ID: " + MessageFormatter.code(String.valueOf(ctx.chatId())));
    }
}
