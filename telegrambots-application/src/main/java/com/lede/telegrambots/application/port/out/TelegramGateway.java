package com.lede.telegrambots.application.port.out;

/**
 * Outbound port for delivering a Telegram HTML message.
 *
 * <p>Application collaborators ({@code BroadcastUseCase}, the command-execution step) depend on
 * this abstraction, not on the concrete HTTP client. Swapping the transport — or stubbing it in
 * tests — touches only the infrastructure adapter, never the callers (Dependency Inversion).</p>
 */
public interface TelegramGateway {

    void sendHtml(String token, long chatId, String html);
}
