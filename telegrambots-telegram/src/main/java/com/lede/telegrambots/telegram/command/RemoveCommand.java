package com.lede.telegrambots.telegram.command;

import com.lede.telegrambots.bot.BotManagementUseCase;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class RemoveCommand implements BotCommand {

    private final BotManagementUseCase bots;

    RemoveCommand(BotManagementUseCase bots) {
        this.bots = bots;
    }

    @Override public String name() { return "/remove"; }

    @Override
    public Optional<String> execute(CommandContext ctx) {
        String botName = ctx.bot().username();
        String requested = stripAt(ctx.arg());
        if (requested.isEmpty() || !requested.equalsIgnoreCase(botName)) {
            return Optional.empty();
        }
        boolean existed = bots.deactivate(ctx.bot(), ctx.chatId());
        return Optional.of(existed
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
