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
public class TelegramClient implements TelegramSender {

    private static final Logger log = LoggerFactory.getLogger(TelegramClient.class);

    private final RestClient http = RestClient.builder()
            .baseUrl("https://api.telegram.org")
            .build();

    @Override
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

    public boolean setWebhook(String token, String url, String secretToken) {
        if (token == null || token.isBlank()) {
            log.warn("Empty bot token — refusing to set webhook");
            return false;
        }
        Map<String, Object> body = Map.of(
                "url", url,
                "secret_token", secretToken
        );
        try {
            log.info("Setting Telegram webhook for bot to {}", url);
            http.post()
                    .uri("/bot{token}/setWebhook", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.error("Telegram setWebhook failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean deleteWebhook(String token) {
        if (token == null || token.isBlank()) {
            log.warn("Empty bot token — refusing to delete webhook");
            return false;
        }
        try {
            log.info("Deleting Telegram webhook");
            http.post()
                    .uri("/bot{token}/deleteWebhook", token)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.error("Telegram deleteWebhook failed: {}", e.getMessage());
            return false;
        }
    }
}
