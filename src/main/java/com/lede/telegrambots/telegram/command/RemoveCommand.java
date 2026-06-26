package com.lede.telegrambots.telegram.command;

import com.lede.telegrambots.bot.BotManagementUseCase;
import com.lede.telegrambots.telegram.TelegramClient;
import org.springframework.stereotype.Component;

@Component
class RemoveCommand implements BotCommand {

    private final TelegramClient telegram;
    private final BotManagementUseCase bots;

    RemoveCommand(TelegramClient telegram, BotManagementUseCase bots) {
        this.telegram = telegram;
        this.bots = bots;
    }

    @Override public String name() { return "/remove"; }

    @Override
    public void execute(CommandContext ctx) {
        String botName = ctx.bot().username();
        String requested = stripAt(ctx.arg());
        if (requested.isEmpty() || !requested.equalsIgnoreCase(botName)) {
            return;
        }
        boolean existed = bots.deactivate(ctx.bot(), ctx.chatId());

        telegram.sendHtml(ctx.bot().token(), ctx.chatId(), existed
                ? "🛑 Đã tắt thông báo trong group này."
                : "ℹ️ Group này vốn không nhận thông báo.");
    }

    private static String stripAt(String s) {
        String t = s.trim();
        if (t.startsWith("@")) t = t.substring(1);
        int sp = t.indexOf(' ');
        return sp >= 0 ? t.substring(0, sp) : t;
    }
}
