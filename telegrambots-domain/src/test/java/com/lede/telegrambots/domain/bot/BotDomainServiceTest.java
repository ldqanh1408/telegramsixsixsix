package com.lede.telegrambots.domain.bot;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BotDomainServiceTest {

    private final BotDomainService domainService = new BotDomainService();

    @Test
    void prepareForSaveThrowsIfUsernameIsBlank() {
        BotRegistration reg = new BotRegistration(null, "token", null, null, null, true);
        assertThatThrownBy(() -> domainService.prepareForSave(null, "", reg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("username is required");
    }

    @Test
    void prepareForSaveThrowsIfTokenIsMissingForNewBot() {
        BotRegistration reg = new BotRegistration("my_bot", null, null, null, null, true);
        assertThatThrownBy(() -> domainService.prepareForSave(null, "my_bot", reg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("token is required for a new bot");
    }

    @Test
    void prepareForSavePreservesExistingValuesIfIncomingNull() {
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
        ManagedBot result = domainService.prepareForSave(existing, "my_bot", reg);

        assertThat(result.id()).isEqualTo("id123");
        assertThat(result.token()).isEqualTo("original-token");
        assertThat(result.tgWebhookSecret()).isEqualTo("tg-sec");
        assertThat(result.githubRepo()).isEqualTo("owner/repo");
        assertThat(result.ghWebhookSecret()).isEqualTo("gh-sec");
        assertThat(result.enabled()).isTrue();
    }

    @Test
    void prepareForSaveGeneratesSecretsIfBlankOrNull() {
        BotRegistration reg = new BotRegistration("my_bot", "token123", null, "owner/repo", "", true);
        ManagedBot result = domainService.prepareForSave(null, "my_bot", reg);

        assertThat(result.username()).isEqualTo("my_bot");
        assertThat(result.token()).isEqualTo("token123");
        assertThat(result.tgWebhookSecret()).isNotBlank().isNotEqualTo("tg-secret");
        assertThat(result.ghWebhookSecret()).isNotBlank().isNotEqualTo("gh-secret");
        assertThat(result.enabled()).isTrue();
    }
}
