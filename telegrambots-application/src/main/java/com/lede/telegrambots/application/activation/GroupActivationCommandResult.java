package com.lede.telegrambots.application.activation;

import com.lede.telegrambots.domain.activation.ActivationResult;

public record GroupActivationCommandResult(Status status, ActivationResult activation) {

    public enum Status {
        MISSING_USERNAME,
        BOT_MISMATCH,
        ACTIVATED,
        ALREADY_ACTIVE,
        DEACTIVATED,
        NOT_ACTIVE
    }

    public static GroupActivationCommandResult missingUsername() {
        return new GroupActivationCommandResult(Status.MISSING_USERNAME, null);
    }

    public static GroupActivationCommandResult botMismatch() {
        return new GroupActivationCommandResult(Status.BOT_MISMATCH, null);
    }

    public static GroupActivationCommandResult activated(ActivationResult activation) {
        return new GroupActivationCommandResult(Status.ACTIVATED, activation);
    }

    public static GroupActivationCommandResult alreadyActive(ActivationResult activation) {
        return new GroupActivationCommandResult(Status.ALREADY_ACTIVE, activation);
    }

    public static GroupActivationCommandResult deactivated() {
        return new GroupActivationCommandResult(Status.DEACTIVATED, null);
    }

    public static GroupActivationCommandResult notActive() {
        return new GroupActivationCommandResult(Status.NOT_ACTIVE, null);
    }
}
