package com.lede.telegrambots.activation;

import com.lede.telegrambots.mongo.entity.GroupActivation;

public record ActivationResult(GroupActivation activation, boolean newlyActivated) {
}
