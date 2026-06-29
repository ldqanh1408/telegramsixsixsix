package com.lede.telegrambots.shared;

/**
 * Outbound port for delivering a Telegram HTML message.
 *
 * <p>Domain/application collaborators ({@code NotificationService}, {@code CommandRouter})
 * depend on this abstraction, not on the concrete {@link TelegramClient} HTTP adapter
 * (Dependency Inversion Principle). Swapping the transport — or stubbing it in tests —
 * touches only the adapter, never the callers.</p>
 */
public interface TelegramSender {

    void sendHtml(String token, long chatId, String html);
}
