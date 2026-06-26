package com.lede.telegrambots.telegram;

import com.lede.telegrambots.mongo.entity.ManagedBot;
import com.lede.telegrambots.bot.BotManagementUseCase;
import com.lede.telegrambots.telegram.dto.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Single multi-bot Telegram webhook. The path variable {@code botUsername} is the
 * dynamic dispatch key — the backend looks up the {@link ManagedBot} in MongoDB and
 * delegates the {@link Update} to the {@link CommandRouter} with that bot's context.
 *
 * <p>Set the webhook URL per bot when registering with Telegram:</p>
 * <pre>POST https://api.telegram.org/bot&lt;TOKEN&gt;/setWebhook
 *   url:         https://your-host/telegram/webhook/&lt;bot-username&gt;
 *   secret_token: &lt;bot.tgWebhookSecret&gt;
 * </pre>
 */
@RestController
@RequestMapping("/telegram/webhook")
public class TelegramWebhookController {

    private static final Logger log = LoggerFactory.getLogger(TelegramWebhookController.class);
    private static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private final BotManagementUseCase bots;
    private final CommandRouter router;

    public TelegramWebhookController(BotManagementUseCase bots, CommandRouter router) {
        this.bots = bots;
        this.router = router;
    }

    @PostMapping("/{botUsername}")
    public ResponseEntity<Void> onUpdate(
            @PathVariable String botUsername,
            @RequestHeader(value = SECRET_HEADER, required = false) String secret,
            @RequestBody Update update) {

        Optional<ManagedBot> maybe = bots.findEnabled(botUsername);
        if (maybe.isEmpty()) {
            log.warn("Telegram webhook for unknown/disabled bot @{}", botUsername);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        ManagedBot bot = maybe.get();

        if (!secretMatches(bot.tgWebhookSecret(), secret)) {
            log.warn("Rejected Telegram webhook for @{}: bad/missing secret token", botUsername);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            router.handle(bot, update);
        } catch (Exception e) {
            log.error("Unhandled error for @{} update {}", botUsername, update.update_id(), e);
        }
        // Always 200 so Telegram does not retry forever.
        return ResponseEntity.ok().build();
    }

    private static boolean secretMatches(String expected, String received) {
        if (expected == null || expected.isBlank()) return true; // verification disabled for this bot
        return expected.equals(received);
    }
}
