package com.lede.telegrambots.telegram.command;

import com.lede.telegrambots.activation.ActivationResult;
import com.lede.telegrambots.bot.BotManagementUseCase;
import com.lede.telegrambots.telegram.MessageFormatter;
import com.lede.telegrambots.telegram.TelegramClient;
import org.springframework.stereotype.Component;

@Component
class AddCommand implements BotCommand {

    private final TelegramClient telegram;
    private final BotManagementUseCase bots;

    AddCommand(TelegramClient telegram, BotManagementUseCase bots) {
        this.telegram = telegram;
        this.bots = bots;
    }

    @Override public String name() { return "/add"; }

    @Override
    public void execute(CommandContext ctx) {
        String botName = ctx.bot().username();
        String token = ctx.bot().token();
        String requested = stripAt(ctx.arg());

        if (requested.isEmpty()) {
            telegram.sendHtml(token, ctx.chatId(),
                    "Cú pháp: " + MessageFormatter.code("/add @" + botName));
            return;
        }
        // The URL path already identifies this bot, but enforce the explicit name
        // so that multi-bot groups (several bots in one chat) only react to their own.
        if (!requested.equalsIgnoreCase(botName)) {
            return;
        }
        ActivationResult result = bots.activate(ctx.bot(), ctx.chatId());
        if (!result.newlyActivated()) {
            telegram.sendHtml(token, ctx.chatId(),
                    "ℹ️ Group này đã được kích hoạt từ trước.");
            return;
        }
        telegram.sendHtml(token, ctx.chatId(),
                "✅ Đã kích hoạt. Sẽ gửi thông báo của repo "
                        + MessageFormatter.code(ctx.bot().githubRepo()) + " vào group này.");
    }

    private static String stripAt(String s) {
        String t = s.trim();
        if (t.startsWith("@")) t = t.substring(1);
        int sp = t.indexOf(' ');
        return sp >= 0 ? t.substring(0, sp) : t;
    }
}
