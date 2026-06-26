package com.lede.telegrambots.bot;

import com.lede.telegrambots.mongo.entity.ManagedBot;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for managed bots — the only storage seam used by {@link ManagedBotRegistry}.
 *
 * <p>The registry depends on this interface, not on Spring Data. The concrete Mongo adapter
 * ({@link MongoBotStore}) hides the repository, so registry/cache logic stays free of
 * persistence details and can be tested without a database.</p>
 */
interface BotStore {

    List<ManagedBot> findAll();

    Optional<ManagedBot> findByUsername(String username);

    ManagedBot save(ManagedBot bot);

    void deleteByUsername(String username);
}
