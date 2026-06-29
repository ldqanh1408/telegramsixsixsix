package com.lede.telegrambots.application.port.out;

import com.lede.telegrambots.domain.bot.ManagedBot;

import java.util.List;
import java.util.Optional;

/**
 * Outbound persistence port for managed bots. The application depends on this abstraction; the
 * Mongo adapter in the infrastructure layer implements it. Method names speak the domain, not
 * Spring Data, keeping the use cases free of persistence details and testable without a database.
 */
public interface BotRepository {

    List<ManagedBot> findAll();

    Optional<ManagedBot> findByUsername(String username);

    ManagedBot save(ManagedBot bot);

    void deleteByUsername(String username);
}
