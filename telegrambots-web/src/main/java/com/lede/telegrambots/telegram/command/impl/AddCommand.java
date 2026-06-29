package com.lede.telegrambots.telegram.command.impl;

import com.lede.telegrambots.application.activation.GroupActivationCommandResult;
import com.lede.telegrambots.application.port.in.BotManagementUseCase;
import com.lede.telegrambots.telegram.command.BotCommand;
import com.lede.telegrambots.telegram.command.CommandContext;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class AddCommand implements BotCommand {

    private final BotManagementUseCase bots;
    private final ActivationCommandPresenter presenter;

    AddCommand(BotManagementUseCase bots, ActivationCommandPresenter presenter) {
        this.bots = bots;
        this.presenter = presenter;
    }

    @Override public String name() { return "/add"; }

    @Override
    public Optional<String> execute(CommandContext ctx) {
        GroupActivationCommandResult result = bots.activateRequested(ctx.bot(), ctx.chatId(), ctx.arg());
        return presenter.activationReply(result, ctx.bot());
    }
}
