package com.lede.telegrambots.activation;

import com.lede.telegrambots.bot.BotUsername;
import com.lede.telegrambots.mongo.entity.GroupActivation;
import com.lede.telegrambots.mongo.entity.ManagedBot;
import com.lede.telegrambots.mongo.repo.GroupActivationRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class GroupBotActivationService {

    private final GroupActivationRepository activations;

    public GroupBotActivationService(GroupActivationRepository activations) {
        this.activations = activations;
    }

    public ActivationResult activate(ManagedBot bot, long chatId) {
        Instant now = Instant.now();
        Optional<GroupActivation> existing = activations.findByBotUsernameAndChatId(bot.username(), chatId);
        if (existing.isPresent() && existing.get().active()) {
            return new ActivationResult(existing.get(), false);
        }

        GroupActivation current = existing.orElse(null);
        GroupActivation saved = activations.save(new GroupActivation(
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
        Optional<GroupActivation> existing = activations.findByBotUsernameAndChatId(bot.username(), chatId);
        if (existing.isEmpty() || !existing.get().active()) {
            return false;
        }

        GroupActivation current = existing.get();
        activations.save(new GroupActivation(
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
        return activations.existsByBotUsernameAndChatIdAndActiveTrue(bot.username(), chatId);
    }

    public long activeGroupCount(ManagedBot bot) {
        return activations.countByBotUsernameAndActiveTrue(bot.username());
    }

    public List<GroupActivation> activeGroups(ManagedBot bot) {
        return activations.findByBotUsernameAndActiveTrue(bot.username());
    }

    public void deleteByBotUsername(String username) {
        activations.deleteByBotUsername(BotUsername.of(username).value());
    }
}
