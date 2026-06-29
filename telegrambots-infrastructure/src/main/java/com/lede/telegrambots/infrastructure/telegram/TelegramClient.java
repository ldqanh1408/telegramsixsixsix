package com.lede.telegrambots.infrastructure.telegram;

import com.lede.telegrambots.application.pipeline.Pipeline;
import com.lede.telegrambots.application.pipeline.Step;
import com.lede.telegrambots.application.port.out.TelegramGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Stateless HTTP adapter for the Telegram Bot API — implements the {@link TelegramGateway} port and
 * additionally offers webhook administration used by {@link TelegramWebhookManager}.
 *
 * <p>The token is passed per call so this single bean can serve any number of dynamically
 * registered bots — token storage is owned by MongoDB, not by this client.</p>
 */
@Component
public class TelegramClient implements TelegramGateway {

    private static final Logger log = LoggerFactory.getLogger(TelegramClient.class);

    private final RestClient http;
    private final Pipeline<SendHtmlContext, Boolean> sendMessagePipeline;
    private final Pipeline<SetWebhookContext, Boolean> setWebhookPipeline;
    private final Pipeline<DeleteWebhookContext, Boolean> deleteWebhookPipeline;

    public TelegramClient() {
        this.http = RestClient.builder()
                .baseUrl("https://api.telegram.org")
                .build();

        this.sendMessagePipeline = new Pipeline<>(List.of(
                new ValidateTokenStep<>("sendMessage"),
                new ExecuteSendMessageStep(http)
        ));
        this.setWebhookPipeline = new Pipeline<>(List.of(
                new ValidateTokenStep<>("setWebhook"),
                new ExecuteSetWebhookStep(http)
        ));
        this.deleteWebhookPipeline = new Pipeline<>(List.of(
                new ValidateTokenStep<>("deleteWebhook"),
                new ExecuteDeleteWebhookStep(http)
        ));
    }

    @Override
    public void sendHtml(String token, long chatId, String html) {
        sendMessagePipeline.run(new SendHtmlContext(token, chatId, html));
    }

    public boolean setWebhook(String token, String url, String secretToken) {
        return setWebhookPipeline.run(new SetWebhookContext(token, url, secretToken))
                .orElse(false);
    }

    public boolean deleteWebhook(String token) {
        return deleteWebhookPipeline.run(new DeleteWebhookContext(token))
                .orElse(false);
    }

    // --- Pipelines Context & Steps ---

    private interface TelegramRequestContext {
        String token();
    }

    private record SendHtmlContext(String token, long chatId, String html) implements TelegramRequestContext {}
    private record SetWebhookContext(String token, String url, String secretToken) implements TelegramRequestContext {}
    private record DeleteWebhookContext(String token) implements TelegramRequestContext {}

    private record ValidateTokenStep<C extends TelegramRequestContext>(String actionName) implements Step<C, Boolean> {
        @Override
        public Optional<Boolean> execute(C ctx) {
            if (ctx.token() == null || ctx.token().isBlank()) {
                log.warn("Empty bot token — refusing to execute {}", actionName);
                return Optional.of(Boolean.FALSE);
            }
            return Optional.empty();
        }
    }

    private record ExecuteSendMessageStep(RestClient http) implements Step<SendHtmlContext, Boolean> {
        @Override
        public Optional<Boolean> execute(SendHtmlContext ctx) {
            Map<String, Object> body = Map.of(
                    "chat_id", ctx.chatId(),
                    "text", ctx.html(),
                    "parse_mode", "HTML",
                    "disable_web_page_preview", true
            );
            try {
                http.post()
                        .uri("/bot{token}/sendMessage", ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .toBodilessEntity();
                return Optional.of(Boolean.TRUE);
            } catch (RestClientException e) {
                log.error("Telegram sendMessage failed for chat {}: {}", ctx.chatId(), e.getMessage());
                return Optional.of(Boolean.FALSE);
            }
        }
    }

    private record ExecuteSetWebhookStep(RestClient http) implements Step<SetWebhookContext, Boolean> {
        @Override
        public Optional<Boolean> execute(SetWebhookContext ctx) {
            Map<String, Object> body = Map.of(
                    "url", ctx.url(),
                    "secret_token", ctx.secretToken()
            );
            try {
                log.info("Setting Telegram webhook for bot to {}", ctx.url());
                http.post()
                        .uri("/bot{token}/setWebhook", ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .toBodilessEntity();
                return Optional.of(Boolean.TRUE);
            } catch (RestClientException e) {
                log.error("Telegram setWebhook failed: {}", e.getMessage());
                return Optional.of(Boolean.FALSE);
            }
        }
    }

    private record ExecuteDeleteWebhookStep(RestClient http) implements Step<DeleteWebhookContext, Boolean> {
        @Override
        public Optional<Boolean> execute(DeleteWebhookContext ctx) {
            try {
                log.info("Deleting Telegram webhook");
                http.post()
                        .uri("/bot{token}/deleteWebhook", ctx.token())
                        .retrieve()
                        .toBodilessEntity();
                return Optional.of(Boolean.TRUE);
            } catch (RestClientException e) {
                log.error("Telegram deleteWebhook failed: {}", e.getMessage());
                return Optional.of(Boolean.FALSE);
            }
        }
    }
}
