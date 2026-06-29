package com.lede.telegrambots.telegram.command.impl;

import com.lede.telegrambots.telegram.command.BotCommand;
import com.lede.telegrambots.telegram.command.CommandContext;



import com.lede.telegrambots.application.port.in.BotManagementUseCase;
import com.lede.telegrambots.domain.shared.MessageFormatter;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class StatusCommand implements BotCommand {

    private final BotManagementUseCase bots;

    StatusCommand(BotManagementUseCase bots) {
        this.bots = bots;
    }

    @Override public String name() { return "/status"; }

    @Override
    public Optional<String> execute(CommandContext ctx) {
        String botName = ctx.bot().username();
        boolean active = bots.isActive(ctx.bot(), ctx.chatId());
        long total = bots.activeGroupCount(ctx.bot());

        return Optional.of("""
                <b>Bot status — @%s</b>
                Repo theo dõi: %s
                Group này: %s
                Tổng số group đang nhận: %d
                """.formatted(
                botName,
                MessageFormatter.code(ctx.bot().githubRepo()),
                active ? "✅ activated" : "⛔ chưa /add",
                total
        ));
    }
}
