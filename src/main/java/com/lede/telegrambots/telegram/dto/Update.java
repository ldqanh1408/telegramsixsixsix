package com.lede.telegrambots.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Update(long update_id, Message message) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(long message_id, Chat chat, User from, String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Chat(long id, String type, String title) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record User(long id, String username, String first_name) {}
}
