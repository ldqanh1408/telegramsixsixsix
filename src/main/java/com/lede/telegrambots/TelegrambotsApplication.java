package com.lede.telegrambots;

import com.lede.telegrambots.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class TelegrambotsApplication {

    public static void main(String[] args) {
        SpringApplication.run(TelegrambotsApplication.class, args);
    }

}
