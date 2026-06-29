package com.lede.telegrambots.domain.bot;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManagedBotTest {

    @Test
    void createThrowsIfUsernameIsBlank() {
        BotRegistration reg = new BotRegistration(null, "token", null, null, null, true);
        assertThatThrownBy(() -> ManagedBot.create("", reg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("username is required");
    }

    @Test
    void createThrowsIfTokenIsMissing() {
        BotRegistration reg = new BotRegistration("my_bot", null, null, null, null, true);
        assertThatThrownBy(() -> ManagedBot.create("my_bot", reg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("token is required for a new bot");
    }

    @Test
    void mergePreservesExistingValuesIfIncomingNull() {
        ManagedBot existing = new ManagedBot(
                "id123",
                "my_bot",
                "original-token",
                "tg-sec",
                "owner/repo",
                "gh-sec",
                true,
                Instant.now(),
                Instant.now()
        );

        BotRegistration reg = new BotRegistration("my_bot", null, null, null, null, null);
        existing.merge(reg);

        assertThat(existing.id()).isEqualTo("id123");
        assertThat(existing.token()).isEqualTo("original-token");
        assertThat(existing.tgWebhookSecret()).isEqualTo("tg-sec");
        assertThat(existing.githubRepo()).isEqualTo("owner/repo");
        assertThat(existing.ghWebhookSecret()).isEqualTo("gh-sec");
        assertThat(existing.enabled()).isTrue();
    }

    @Test
    void createGeneratesSecretsIfBlankOrNull() {
        BotRegistration reg = new BotRegistration("my_bot", "token123", null, "owner/repo", "", true);
        ManagedBot result = ManagedBot.create("my_bot", reg);

        assertThat(result.username()).isEqualTo("my_bot");
        assertThat(result.token()).isEqualTo("token123");
        assertThat(result.tgWebhookSecret()).isNotBlank().isNotEqualTo("");
        assertThat(result.ghWebhookSecret()).isNotBlank().isNotEqualTo("");
        assertThat(result.enabled()).isTrue();
    }
}
