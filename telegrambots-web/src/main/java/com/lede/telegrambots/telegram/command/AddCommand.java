package com.lede.telegrambots.telegram.command;

import com.lede.telegrambots.activation.ActivationResult;
import com.lede.telegrambots.bot.BotManagementUseCase;
import com.lede.telegrambots.shared.MessageFormatter;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class AddCommand implements BotCommand {

    private final BotManagementUseCase bots;

    AddCommand(BotManagementUseCase bots) {
        this.bots = bots;
    }

    @Override public String name() { return "/add"; }

    @Override
    public Optional<String> execute(CommandContext ctx) {
        String botName = ctx.bot().username();
        String requested = stripAt(ctx.arg());

        if (requested.isEmpty()) {
            return Optional.of("Cú pháp: " + MessageFormatter.code("/add @" + botName));
        }
        // The URL path already identifies this bot, but enforce the explicit name
        // so that multi-bot groups (several bots in one chat) only react to their own.
        if (!requested.equalsIgnoreCase(botName)) {
            return Optional.empty();
        }
        ActivationResult result = bots.activate(ctx.bot(), ctx.chatId());
        if (!result.newlyActivated()) {
            return Optional.of("ℹ️ Group này đã được kích hoạt từ trước.");
        }
        return Optional.of("✅ Đã kích hoạt. Sẽ gửi thông báo của repo "
                + MessageFormatter.code(ctx.bot().githubRepo()) + " vào group này.");
    }

    private static String stripAt(String s) {
        String t = s.trim();
        if (t.startsWith("@")) t = t.substring(1);
        int sp = t.indexOf(' ');
        return sp >= 0 ? t.substring(0, sp) : t;
    }
}
