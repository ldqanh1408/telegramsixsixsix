package com.lede.telegrambots.telegram.command;

import com.lede.telegrambots.bot.BotManagementUseCase;
import com.lede.telegrambots.telegram.MessageFormatter;
import com.lede.telegrambots.telegram.TelegramClient;
import org.springframework.stereotype.Component;

@Component
class StatusCommand implements BotCommand {

    private final TelegramClient telegram;
    private final BotManagementUseCase bots;

    StatusCommand(TelegramClient telegram, BotManagementUseCase bots) {
        this.telegram = telegram;
        this.bots = bots;
    }

    @Override public String name() { return "/status"; }

    @Override
    public void execute(CommandContext ctx) {
        String botName = ctx.bot().username();
        boolean active = bots.isActive(ctx.bot(), ctx.chatId());
        long total = bots.activeGroupCount(ctx.bot());

        String body = """
                <b>Bot status — @%s</b>
                Repo theo dõi: %s
                Group này: %s
                Tổng số group đang nhận: %d
                """.formatted(
                botName,
                MessageFormatter.code(ctx.bot().githubRepo()),
                active ? "✅ activated" : "⛔ chưa /add",
                total
        );
        telegram.sendHtml(ctx.bot().token(), ctx.chatId(), body);
    }
}
