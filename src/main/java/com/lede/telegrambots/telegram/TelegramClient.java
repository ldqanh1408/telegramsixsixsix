package com.lede.telegrambots.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Stateless HTTP client for the Telegram Bot API.
 *
 * <p>The token is passed per call so this single bean can serve any number of
 * dynamically registered bots — token storage is owned by MongoDB, not by this client.</p>
 */
@Component
public class TelegramClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramClient.class);

    private final RestClient http = RestClient.builder()
            .baseUrl("https://api.telegram.org")
            .build();

    public void sendHtml(String token, long chatId, String html) {
        if (token == null || token.isBlank()) {
            log.warn("Empty bot token — refusing to send to chat {}", chatId);
            return;
        }
        Map<String, Object> body = Map.of(
                "chat_id", chatId,
                "text", html,
                "parse_mode", "HTML",
                "disable_web_page_preview", true
        );
        try {
            http.post()
                    .uri("/bot{token}/sendMessage", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Telegram sendMessage failed for chat {}: {}", chatId, e.getMessage());
        }
    }
}
