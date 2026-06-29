package com.lede.telegrambots.telegram.command;

import com.lede.telegrambots.activation.ActivationResult;
import com.lede.telegrambots.bot.BotManagementUseCase;
import com.lede.telegrambots.mongo.entity.ManagedBot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Commands are pure functions of their context — testable without any transport
 * ({@code TelegramClient}/{@code TelegramSender}), which is the point of returning the
 * reply instead of sending it.
 */
class AddCommandTest {

    private final BotManagementUseCase bots = mock(BotManagementUseCase.class);
    private final AddCommand command = new AddCommand(bots);

    private static ManagedBot bot() {
        return new ManagedBot(null, "my_bot", "tok", null, "owner/repo", null, true,
                Instant.now(), Instant.now());
    }

    private CommandContext ctx(String arg) {
        return new CommandContext(-100L, arg, bot());
    }

    @Test
    void emptyArgReturnsSyntaxHelp() {
        Optional<String> reply = command.execute(ctx(""));
        assertTrue(reply.isPresent());
        assertTrue(reply.get().contains("/add @my_bot"));
    }

    @Test
    void differentBotNameStaysSilent() {
        Optional<String> reply = command.execute(ctx("@other_bot"));
        assertEquals(Optional.empty(), reply);
    }

    @Test
    void newlyActivatedConfirms() {
        when(bots.activate(any(ManagedBot.class), anyLong())).thenReturn(new ActivationResult(null, true));
        Optional<String> reply = command.execute(ctx("@my_bot"));
        assertTrue(reply.isPresent());
        assertTrue(reply.get().contains("✅"));
    }

    @Test
    void alreadyActiveInforms() {
        when(bots.activate(any(ManagedBot.class), anyLong())).thenReturn(new ActivationResult(null, false));
        Optional<String> reply = command.execute(ctx("@my_bot"));
        assertTrue(reply.isPresent());
        assertTrue(reply.get().contains("ℹ️"));
    }
}
