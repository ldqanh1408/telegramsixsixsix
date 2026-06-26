package com.lede.telegrambots.telegram.command;

import com.lede.telegrambots.telegram.TelegramClient;
import org.springframework.stereotype.Component;

@Component
class HelpCommand implements BotCommand {

    private final TelegramClient telegram;

    HelpCommand(TelegramClient telegram) {
        this.telegram = telegram;
    }

    @Override public String name() { return "/help"; }

    @Override
    public void execute(CommandContext ctx) {
        String botName = ctx.bot().username();
        String body = """
                <b>Lệnh khả dụng</b>
                /start — chào
                /help  — danh sách lệnh
                /id    — trả về Chat ID của group
                /status — trạng thái bot và group này
                /add @%s    — kích hoạt bot trong group này
                /remove @%s — tắt thông báo trong group này
                """.formatted(botName, botName);
        telegram.sendHtml(ctx.bot().token(), ctx.chatId(), body);
    }
}
