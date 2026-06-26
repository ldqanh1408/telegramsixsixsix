package com.lede.telegrambots.github;

import com.lede.telegrambots.mongo.entity.ManagedBot;
import com.lede.telegrambots.bot.BotManagementUseCase;
import com.lede.telegrambots.notification.NotificationService;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@RestController
@RequestMapping("/github/webhook")
public class GitHubWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookController.class);

    private final BotManagementUseCase bots;
    private final NotificationService notifications;
    private final ObjectMapper mapper;

    public GitHubWebhookController(BotManagementUseCase bots,
                                   NotificationService notifications,
                                   ObjectMapper mapper) {
        this.bots = bots;
        this.notifications = notifications;
        this.mapper = mapper;
    }

    @PostMapping("/{botUsername}")
    public ResponseEntity<String> onEvent(
            @PathVariable String botUsername,
            @RequestHeader(value = "X-GitHub-Event", required = false) String event,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String delivery,
            @RequestBody byte[] body) {

        Optional<ManagedBot> maybe = bots.findEnabled(botUsername);
        if (maybe.isEmpty()) {
            log.warn("GitHub webhook delivery={} for unknown/disabled bot @{}", delivery, botUsername);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("unknown bot");
        }
        ManagedBot bot = maybe.get();

        if (!SignatureVerifier.verify(bot.ghWebhookSecret(), signature, body)) {
            log.warn("Rejected GitHub webhook delivery={} for @{}: bad signature", delivery, bot.username());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("bad signature");
        }

        if (event == null) {
            return ResponseEntity.badRequest().body("missing X-GitHub-Event");
        }

        if ("ping".equals(event)) {
            return ResponseEntity.ok("pong");
        }

        JsonNode payload;
        try {
            payload = mapper.readTree(body);
        } catch (Exception e) {
            log.warn("Invalid JSON in webhook delivery={} for @{}", delivery, bot.username(), e);
            return ResponseEntity.badRequest().body("invalid json");
        }

        if (!matchesConfiguredRepo(bot, payload)) {
            log.debug("Ignored event={} for repo={} on @{} (configured={})",
                    event,
                    payload.path("repository").path("full_name").asText(""),
                    bot.username(),
                    bot.githubRepo());
            return ResponseEntity.ok("ignored: repo mismatch");
        }

        try {
            notifications.dispatch(bot, event, payload);
        } catch (Exception e) {
            log.error("Error dispatching event={} delivery={} for @{}", event, delivery, bot.username(), e);
        }
        return ResponseEntity.ok("ok");
    }

    private boolean matchesConfiguredRepo(ManagedBot bot, JsonNode payload) {
        if (bot.githubRepo() == null || bot.githubRepo().isBlank()) return true;
        String fullName = payload.path("repository").path("full_name").asText("");
        return bot.githubRepo().equalsIgnoreCase(fullName);
    }
}
