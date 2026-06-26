package com.lede.telegrambots.bot;

import com.lede.telegrambots.mongo.entity.ManagedBot;
import com.lede.telegrambots.mongo.repo.ManagedBotRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManagedBotRegistryTest {

    @Test
    void loadEnabledBotsCachesEnabledBotsByUsername() {
        ManagedBotRepository bots = mock(ManagedBotRepository.class);
        ManagedBot enabled = bot("helper_bot", true);
        ManagedBot disabled = bot("disabled_bot", false);
        when(bots.findAll()).thenReturn(List.of(enabled, disabled));

        ManagedBotRegistry registry = new ManagedBotRegistry(bots);
        registry.loadEnabledBots();

        assertThat(registry.findEnabled("@HELPER_BOT")).contains(enabled);
        verify(bots, never()).findByUsername("helper_bot");
    }

    @Test
    void upsertNormalizesUsernameAndAddsEnabledBotToRegistry() {
        ManagedBotRepository bots = mock(ManagedBotRepository.class);
        when(bots.findByUsername("my_repo_bot")).thenReturn(Optional.empty());
        when(bots.save(any(ManagedBot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ManagedBotRegistry registry = new ManagedBotRegistry(bots);
        ManagedBot saved = registry.upsert(new BotRegistration(
                "@My_Repo_Bot",
                "123:ABC",
                "tg-secret",
                "owner/repo",
                "gh-secret",
                true
        ));

        assertThat(saved.username()).isEqualTo("my_repo_bot");
        assertThat(registry.findEnabled("MY_REPO_BOT")).contains(saved);
    }

    private static ManagedBot bot(String username, boolean enabled) {
        Instant now = Instant.parse("2026-06-25T00:00:00Z");
        return new ManagedBot(
                username + "-id",
                username,
                "123:ABC",
                "tg-secret",
                "owner/repo",
                "gh-secret",
                enabled,
                now,
                now
        );
    }
}
