package com.lede.telegrambots.bot;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BotUsernameTest {

    @Test
    void normalizesTelegramBotUsername() {
        assertThat(BotUsername.of("  @@My_Repo_Bot  ").value())
                .isEqualTo("my_repo_bot");
    }

    @Test
    void nullUsernameBecomesBlankValue() {
        assertThat(BotUsername.of(null).isBlank()).isTrue();
    }
}
