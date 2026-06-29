package com.lede.telegrambots.infrastructure.telegram;

import com.lede.telegrambots.domain.bot.BotDeletedEvent;
import com.lede.telegrambots.domain.bot.BotSavedEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class TelegramWebhookManagerTest {

    @Test
    void onBotSavedEnabledCallsSetWebhook() {
        TelegramClient client = mock(TelegramClient.class);
        TelegramWebhookManager manager = new TelegramWebhookManager(client, "https://my-domain.com");

        manager.onBotSaved(new BotSavedEvent("my_bot", "token123", "tg-secret", true));

        verify(client).setWebhook("token123", "https://my-domain.com/telegram/webhook/my_bot", "tg-secret");
        verify(client, never()).deleteWebhook(anyString());
    }

    @Test
    void onBotSavedDisabledCallsDeleteWebhook() {
        TelegramClient client = mock(TelegramClient.class);
        TelegramWebhookManager manager = new TelegramWebhookManager(client, "https://my-domain.com");

        manager.onBotSaved(new BotSavedEvent("my_bot", "token123", "tg-secret", false));

        verify(client).deleteWebhook("token123");
        verify(client, never()).setWebhook(anyString(), anyString(), anyString());
    }

    @Test
    void onBotSavedEnabledSkipsIfPublicUrlEmpty() {
        TelegramClient client = mock(TelegramClient.class);
        TelegramWebhookManager manager = new TelegramWebhookManager(client, "");

        manager.onBotSaved(new BotSavedEvent("my_bot", "token123", "tg-secret", true));

        verify(client, never()).setWebhook(anyString(), anyString(), anyString());
        verify(client, never()).deleteWebhook(anyString());
    }

    @Test
    void onBotDeletedCallsDeleteWebhook() {
        TelegramClient client = mock(TelegramClient.class);
        TelegramWebhookManager manager = new TelegramWebhookManager(client, "https://my-domain.com");

        manager.onBotDeleted(new BotDeletedEvent("my_bot", "token123"));

        verify(client).deleteWebhook("token123");
        verify(client, never()).setWebhook(anyString(), anyString(), anyString());
    }
}
