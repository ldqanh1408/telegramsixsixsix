package com.lede.telegrambots.telegram.command.impl;

import com.lede.telegrambots.application.activation.GroupActivationCommandResult;
import com.lede.telegrambots.domain.bot.ManagedBot;
import com.lede.telegrambots.domain.shared.MessageFormatter;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class ActivationCommandPresenter {

    Optional<String> activationReply(GroupActivationCommandResult result, ManagedBot bot) {
        return switch (result.status()) {
            case MISSING_USERNAME -> Optional.of("Cú pháp: " + MessageFormatter.code("/add @" + bot.username()));
            case BOT_MISMATCH -> Optional.empty();
            case ALREADY_ACTIVE -> Optional.of("ℹ️ Group này đã được kích hoạt từ trước.");
            case ACTIVATED -> Optional.of("✅ Đã kích hoạt. Sẽ gửi thông báo của repo "
                    + MessageFormatter.code(bot.githubRepo()) + " vào group này.");
            case DEACTIVATED, NOT_ACTIVE -> Optional.empty();
        };
    }

    Optional<String> deactivationReply(GroupActivationCommandResult result) {
        return switch (result.status()) {
            case DEACTIVATED -> Optional.of("🛑 Đã tắt thông báo trong group này.");
            case NOT_ACTIVE -> Optional.of("ℹ️ Group này vốn không nhận thông báo.");
            case MISSING_USERNAME, BOT_MISMATCH -> Optional.empty();
            case ACTIVATED, ALREADY_ACTIVE -> Optional.empty();
        };
    }
}
