package com.lede.telegrambots.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Admin admin) {

    public AppProperties {
        if (admin == null) admin = new Admin("");
    }

    public record Admin(String token) {}
}
