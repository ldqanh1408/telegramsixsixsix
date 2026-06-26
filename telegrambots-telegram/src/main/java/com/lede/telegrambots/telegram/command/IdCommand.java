package com.lede.telegrambots.telegram.command;

import com.lede.telegrambots.shared.MessageFormatter;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class IdCommand implements BotCommand {

    @Override public String name() { return "/id"; }

    @Override
    public Optional<String> execute(CommandContext ctx) {
        return Optional.of("Chat ID: " + MessageFormatter.code(String.valueOf(ctx.chatId())));
    }
}
