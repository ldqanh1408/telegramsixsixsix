package com.lede.telegrambots.application.notification;

import com.lede.telegrambots.application.pipeline.Pipeline;
import com.lede.telegrambots.application.port.out.ActivationRepository;
import com.lede.telegrambots.application.port.out.TelegramGateway;
import com.lede.telegrambots.application.notification.steps.*;
import com.lede.telegrambots.domain.bot.ManagedBot;
import java.util.List;

public class BroadcastUseCase {

    private final Pipeline<BroadcastContext, Boolean> pipeline;

    public BroadcastUseCase(TelegramGateway telegram, ActivationRepository activations) {
        this.pipeline = new Pipeline<>(List.of(
                new LoadTargetsStep(activations),
                new ValidateTargetsStep(),
                new DeliverMessagesStep(telegram)
        ));
    }

    public void broadcast(ManagedBot bot, String html) {
        pipeline.run(new BroadcastContext(bot, html));
    }
}
