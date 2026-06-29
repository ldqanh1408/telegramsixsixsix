package com.lede.telegrambots.telegram.command.impl;

import com.lede.telegrambots.application.activation.GroupActivationCommandResult;
import com.lede.telegrambots.application.port.in.BotManagementUseCase;
import com.lede.telegrambots.domain.bot.ManagedBot;
import com.lede.telegrambots.telegram.command.CommandContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RemoveCommandTest {

    private final BotManagementUseCase bots = mock(BotManagementUseCase.class);
    private final RemoveCommand command = new RemoveCommand(bots, new ActivationCommandPresenter());

    @Test
    void differentBotNameStaysSilent() {
        when(bots.deactivateRequested(any(ManagedBot.class), anyLong(), any()))
                .thenReturn(GroupActivationCommandResult.botMismatch());

        Optional<String> reply = command.execute(ctx("@other_bot"));

        assertEquals(Optional.empty(), reply);
    }

    @Test
    void deactivatedConfirms() {
        when(bots.deactivateRequested(any(ManagedBot.class), anyLong(), any()))
                .thenReturn(GroupActivationCommandResult.deactivated());

        Optional<String> reply = command.execute(ctx("@my_bot"));

        assertTrue(reply.isPresent());
        assertTrue(reply.get().contains("Đã tắt"));
    }

    @Test
    void notActiveInforms() {
        when(bots.deactivateRequested(any(ManagedBot.class), anyLong(), any()))
                .thenReturn(GroupActivationCommandResult.notActive());

        Optional<String> reply = command.execute(ctx("@my_bot"));

        assertTrue(reply.isPresent());
        assertTrue(reply.get().contains("không nhận"));
    }

    private static CommandContext ctx(String arg) {
        return new CommandContext(-100L, arg, bot());
    }

    private static ManagedBot bot() {
        return new ManagedBot(null, "my_bot", "tok", null, "owner/repo", null, true,
                Instant.now(), Instant.now());
    }
}
