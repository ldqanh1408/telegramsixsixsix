package com.lede.telegrambots.application.notification.steps;

import com.lede.telegrambots.application.notification.BroadcastContext;
import com.lede.telegrambots.application.notification.BroadcastStep;
import com.lede.telegrambots.application.port.out.TelegramGateway;
import com.lede.telegrambots.domain.activation.GroupActivation;
import java.util.Optional;

public record DeliverMessagesStep(TelegramGateway telegram) implements BroadcastStep {
    @Override
    public Optional<Boolean> execute(BroadcastContext ctx) {
        for (GroupActivation activation : ctx.getTargets()) {
            telegram.sendHtml(ctx.getBot().token(), activation.chatId(), ctx.getHtml());
        }
        return Optional.of(Boolean.TRUE);
    }
}
