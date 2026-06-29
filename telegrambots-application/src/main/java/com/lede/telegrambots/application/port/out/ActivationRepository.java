package com.lede.telegrambots.application.port.out;

import com.lede.telegrambots.domain.activation.GroupActivation;

import java.util.List;
import java.util.Optional;

/**
 * Outbound persistence port for group activations. Implemented by the Mongo adapter in the
 * infrastructure layer; the activation use cases depend only on this interface.
 */
public interface ActivationRepository {

    Optional<GroupActivation> find(String botUsername, long chatId);

    GroupActivation save(GroupActivation activation);

    boolean isActive(String botUsername, long chatId);

    long countActive(String botUsername);

    List<GroupActivation> findActive(String botUsername);

    void deleteAllFor(String botUsername);
}
