package com.lede.telegrambots.activation;

import com.lede.telegrambots.mongo.entity.GroupActivation;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for group activations — the only way {@link GroupBotActivationService}
 * touches storage. Method names speak the domain ("isActive", "countActive"), not Spring Data.
 *
 * <p>The business service depends on this interface; the concrete Mongo adapter
 * ({@link MongoActivationStore}) is the only place that knows about Spring Data. This keeps
 * activation rules free of persistence details and trivially unit-testable.</p>
 */
interface ActivationStore {

    Optional<GroupActivation> find(String botUsername, long chatId);

    GroupActivation save(GroupActivation activation);

    boolean isActive(String botUsername, long chatId);

    long countActive(String botUsername);

    List<GroupActivation> findActive(String botUsername);

    void deleteAllFor(String botUsername);
}
