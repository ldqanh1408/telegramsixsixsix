package com.lede.telegrambots.application.notification;

import com.lede.telegrambots.application.port.out.ActivationRepository;
import com.lede.telegrambots.application.port.out.TelegramGateway;
import com.lede.telegrambots.domain.activation.GroupActivation;
import com.lede.telegrambots.domain.bot.ManagedBot;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;

/**
 * Generic broadcaster: delivers an already-rendered HTML message to every active Telegram group
 * of a managed bot. Source-agnostic — it knows nothing about GitHub. Event rendering lives in the
 * web layer ({@code GitHubEventRenderer}); this use case only fans the result out.
 */
public class BroadcastUseCase {

    private static final Logger log = System.getLogger(BroadcastUseCase.class.getName());

    private final TelegramGateway telegram;
    private final ActivationRepository activations;

    public BroadcastUseCase(TelegramGateway telegram, ActivationRepository activations) {
        this.telegram = telegram;
        this.activations = activations;
    }

    /** Fan out one HTML message to all groups where this bot is active. */
    public void broadcast(ManagedBot bot, String html) {
        List<GroupActivation> targets = activations.findActive(bot.username());
        if (targets.isEmpty()) {
            log.log(Level.INFO, "Message for @{0} but no activated groups - dropped", bot.username());
            return;
        }
        for (GroupActivation activation : targets) {
            telegram.sendHtml(bot.token(), activation.chatId(), html);
        }
    }
}
