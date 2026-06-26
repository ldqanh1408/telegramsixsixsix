package com.lede.telegrambots.telegram.command;

import com.lede.telegrambots.shared.MessageFormatter;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class StartCommand implements BotCommand {

    @Override public String name() { return "/start"; }

    @Override
    public Optional<String> execute(CommandContext ctx) {
        return Optional.of(
                "Xin chào! Tôi là bot thông báo GitHub cho repo "
                        + MessageFormatter.code(ctx.bot().githubRepo()) + ".\n"
                        + "Dùng " + MessageFormatter.code("/help") + " để xem các lệnh.");
    }
}
