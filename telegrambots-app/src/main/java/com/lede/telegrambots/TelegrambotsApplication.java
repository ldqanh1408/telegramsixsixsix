package com.lede.telegrambots;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point. Component scanning across {@code com.lede.telegrambots.*} picks up the
 * web controllers and the infrastructure adapters; the pure application use cases are assembled
 * explicitly in {@link UseCaseConfiguration}.
 */
@SpringBootApplication
public class TelegrambotsApplication {

    public static void main(String[] args) {
        SpringApplication.run(TelegrambotsApplication.class, args);
    }

}
