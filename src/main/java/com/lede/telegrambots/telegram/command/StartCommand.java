package com.lede.telegrambots.telegram.command;

import com.lede.telegrambots.telegram.MessageFormatter;
import com.lede.telegrambots.telegram.TelegramClient;
import org.springframework.stereotype.Component;

@Component
class StartCommand implements BotCommand {

    private final TelegramClient telegram;

    StartCommand(TelegramClient telegram) {
        this.telegram = telegram;
    }

    @Override public String name() { return "/start"; }

    @Override
    public void execute(CommandContext ctx) {
        telegram.sendHtml(ctx.bot().token(), ctx.chatId(),
                "Xin chào! Tôi là bot thông báo GitHub cho repo "
                        + MessageFormatter.code(ctx.bot().githubRepo()) + ".\n"
                        + "Dùng " + MessageFormatter.code("/help") + " để xem các lệnh.");
    }
}
