package com.lede.telegrambots.telegram;

import com.lede.telegrambots.bot.BotManagementUseCase;
import com.lede.telegrambots.mongo.entity.ManagedBot;
import com.lede.telegrambots.telegram.command.BotCommand;
import com.lede.telegrambots.telegram.dto.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TelegramWebhookProcessorTest {

    private BotManagementUseCase bots;
    private TelegramSender sender;
    private BotCommand mockCommand;
    private TelegramWebhookProcessor processor;

    @BeforeEach
    void setUp() {
        bots = mock(BotManagementUseCase.class);
        sender = mock(TelegramSender.class);
        mockCommand = mock(BotCommand.class);
        when(mockCommand.name()).thenReturn("/test");

        List<TelegramWebhookStep> steps = List.of(
                new TelegramBotLookupStep(bots),
                new TelegramSecretVerificationStep(),
                new TelegramMessageValidationStep(),
                new TelegramCommandParsingStep(),
                new TelegramCommandLookupStep(List.of(mockCommand)),
                new TelegramCommandExecutionStep(sender)
        );

        processor = new TelegramWebhookProcessor(steps);
    }

    private static ManagedBot bot() {
        return new ManagedBot(null, "my_bot", "tok", "tg-secret", null, null, true,
                Instant.now(), Instant.now());
    }

    private static Update update(String text) {
        Update.Chat chat = new Update.Chat(-123L, "group", "Test Group");
        Update.Message message = new Update.Message(1001L, chat, null, text);
        return new Update(9999L, message);
    }

    @Test
    void unknownBotReturnsUnknownBot() {
        when(bots.findEnabled("ghost")).thenReturn(Optional.empty());

        TelegramWebhookResult r = processor.process("ghost", "tg-secret", update("/test"));

        assertEquals(TelegramWebhookResult.Outcome.UNKNOWN_BOT, r.outcome());
    }

    @Test
    void badSecretReturnsBadSecret() {
        when(bots.findEnabled("my_bot")).thenReturn(Optional.of(bot()));

        TelegramWebhookResult r = processor.process("my_bot", "wrong-secret", update("/test"));

        assertEquals(TelegramWebhookResult.Outcome.BAD_SECRET, r.outcome());
    }

    @Test
    void invalidMessageReturnsInvalidMessage() {
        when(bots.findEnabled("my_bot")).thenReturn(Optional.of(bot()));
        Update update = new Update(9999L, null); // null message

        TelegramWebhookResult r = processor.process("my_bot", "tg-secret", update);

        assertEquals(TelegramWebhookResult.Outcome.INVALID_MESSAGE, r.outcome());
    }

    @Test
    void invalidCommandReturnsInvalidCommand() {
        when(bots.findEnabled("my_bot")).thenReturn(Optional.of(bot()));

        TelegramWebhookResult r = processor.process("my_bot", "tg-secret", update("hello"));

        assertEquals(TelegramWebhookResult.Outcome.INVALID_COMMAND, r.outcome());
    }

    @Test
    void unknownCommandReturnsUnknownCommand() {
        when(bots.findEnabled("my_bot")).thenReturn(Optional.of(bot()));

        TelegramWebhookResult r = processor.process("my_bot", "tg-secret", update("/unknown"));

        assertEquals(TelegramWebhookResult.Outcome.UNKNOWN_COMMAND, r.outcome());
    }

    @Test
    void matchingCommandExecutesCommandAndReturnsOk() {
        when(bots.findEnabled("my_bot")).thenReturn(Optional.of(bot()));
        when(mockCommand.execute(any())).thenReturn(Optional.of("html reply"));

        TelegramWebhookResult r = processor.process("my_bot", "tg-secret", update("/test"));

        assertEquals(TelegramWebhookResult.Outcome.OK, r.outcome());
        verify(sender).sendHtml("tok", -123L, "html reply");
    }
}
