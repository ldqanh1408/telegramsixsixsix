package com.lede.telegrambots.telegram.impl;

import com.lede.telegrambots.telegram.impl.TelegramWebhookResult.Outcome;

import com.lede.telegrambots.telegram.*;


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

/**
 * Single multi-bot Telegram webhook controller.
 * Delegates the processing pipeline to {@link TelegramWebhookProcessor}.
 */
@RestController
@RequestMapping("/telegram/webhook")
public class TelegramWebhookController {

    private static final Logger log = LoggerFactory.getLogger(TelegramWebhookController.class);
    private static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private final TelegramWebhookProcessor processor;

    public TelegramWebhookController(TelegramWebhookProcessor processor) {
        this.processor = processor;
    }

    @PostMapping("/{botUsername}")
    public ResponseEntity<Void> onUpdate(
            @PathVariable String botUsername,
            @RequestHeader(value = SECRET_HEADER, required = false) String secret,
            @RequestBody Update update) {

        TelegramWebhookResult result = processor.process(botUsername, secret, update);

        if (result.outcome() == Outcome.UNKNOWN_BOT) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (result.outcome() == Outcome.BAD_SECRET) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Always 200 so Telegram does not retry forever.
        return ResponseEntity.ok().build();
    }
}
