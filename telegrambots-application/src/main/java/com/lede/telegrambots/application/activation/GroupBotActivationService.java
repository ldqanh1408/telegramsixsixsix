package com.lede.telegrambots.application.activation;

import com.lede.telegrambots.application.port.out.ActivationRepository;
import com.lede.telegrambots.domain.activation.ActivationResult;
import com.lede.telegrambots.domain.activation.GroupActivation;
import com.lede.telegrambots.domain.bot.ManagedBot;
import com.lede.telegrambots.domain.shared.BotUsername;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Group activation business rules. Talks to storage only through {@link ActivationRepository},
 * so it is pure application logic with no persistence coupling.
 */
public class GroupBotActivationService {

    private final ActivationRepository store;

    public GroupBotActivationService(ActivationRepository store) {
        this.store = store;
    }

    public ActivationResult activate(ManagedBot bot, long chatId) {
        Instant now = Instant.now();
        Optional<GroupActivation> existing = store.find(bot.username(), chatId);
        if (existing.isPresent() && existing.get().active()) {
            return new ActivationResult(existing.get(), false);
        }

        GroupActivation current = existing.orElse(null);
        GroupActivation saved = store.save(new GroupActivation(
                current == null ? null : current.id(),
                bot.id(),
                bot.username(),
                chatId,
                true,
                current == null || current.activatedAt() == null ? now : current.activatedAt(),
                now
        ));
        return new ActivationResult(saved, true);
    }

    public boolean deactivate(ManagedBot bot, long chatId) {
        Optional<GroupActivation> existing = store.find(bot.username(), chatId);
        if (existing.isEmpty() || !existing.get().active()) {
            return false;
        }

        GroupActivation current = existing.get();
        store.save(new GroupActivation(
                current.id(),
                current.botId(),
                current.botUsername(),
                current.chatId(),
                false,
                current.activatedAt(),
                Instant.now()
        ));
        return true;
    }

    public boolean isActive(ManagedBot bot, long chatId) {
        return store.isActive(bot.username(), chatId);
    }

    public long activeGroupCount(ManagedBot bot) {
        return store.countActive(bot.username());
    }

    public List<GroupActivation> activeGroups(ManagedBot bot) {
        return store.findActive(bot.username());
    }

    public void deleteByBotUsername(String username) {
        store.deleteAllFor(BotUsername.of(username).value());
    }
}
