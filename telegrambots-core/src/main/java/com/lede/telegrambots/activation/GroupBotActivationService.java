package com.lede.telegrambots.activation;

import com.lede.telegrambots.mongo.entity.GroupActivation;
import com.lede.telegrambots.mongo.entity.ManagedBot;
import com.lede.telegrambots.shared.BotUsername;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Group activation business rules. Talks to storage only through {@link ActivationStore},
 * so this class is pure domain logic with no Spring Data coupling.
 */
@Service
public class GroupBotActivationService {

    private final ActivationStore store;

    public GroupBotActivationService(ActivationStore store) {
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
