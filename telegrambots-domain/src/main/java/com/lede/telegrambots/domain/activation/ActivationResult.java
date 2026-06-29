package com.lede.telegrambots.domain.activation;

/**
 * Outcome of an activation attempt: the resulting {@link GroupActivation} plus whether this
 * call is what flipped it on ({@code newlyActivated == false} means it was already active).
 */
public record ActivationResult(GroupActivation activation, boolean newlyActivated) {
}
