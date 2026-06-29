package com.lede.telegrambots.application.notification;

import com.lede.telegrambots.domain.pipeline.Pipeline;
import com.lede.telegrambots.application.port.out.ActivationRepository;
import com.lede.telegrambots.application.port.out.TelegramGateway;
import com.lede.telegrambots.application.notification.steps.*;
import com.lede.telegrambots.domain.bot.ManagedBot;
import java.util.List;

public class BroadcastUseCase {

    private final Pipeline<BroadcastContext, Boolean> pipeline;

    // Normal constructor
    public BroadcastUseCase(TelegramGateway telegram, ActivationRepository activations) {
        this.pipeline = new Pipeline<>(List.of(
                new LoadTargetsStep(activations),
                new ValidateTargetsStep(),
                new DeliverMessagesStep(telegram)
        ));
    }

    // Spring autowired constructor
    public BroadcastUseCase(List<BroadcastStep> steps) {
        this.pipeline = new Pipeline<>(steps);
    }

    public void broadcast(ManagedBot bot, String html) {
        pipeline.run(new BroadcastContext(bot, html));
    }
}
