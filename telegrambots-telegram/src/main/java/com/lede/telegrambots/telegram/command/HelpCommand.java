package com.lede.telegrambots.telegram.command;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class HelpCommand implements BotCommand {

    @Override public String name() { return "/help"; }

    @Override
    public Optional<String> execute(CommandContext ctx) {
        String botName = ctx.bot().username();
        return Optional.of("""
                <b>Lệnh khả dụng</b>
                /start — chào
                /help  — danh sách lệnh
                /id    — trả về Chat ID của group
                /status — trạng thái bot và group này
                /add @%s    — kích hoạt bot trong group này
                /remove @%s — tắt thông báo trong group này
                """.formatted(botName, botName));
    }
}
