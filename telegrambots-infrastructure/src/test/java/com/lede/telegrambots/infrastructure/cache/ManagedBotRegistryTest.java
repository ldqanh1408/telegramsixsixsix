package com.lede.telegrambots.infrastructure.cache;

import com.lede.telegrambots.application.port.out.BotRepository;
import com.lede.telegrambots.domain.bot.ManagedBot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ManagedBotRegistryTest {

    @Test
    void loadEnabledBotsCachesEnabledBotsByUsername() {
        BotRepository bots = mock(BotRepository.class);
        ManagedBot enabled = bot("helper_bot", true);
        ManagedBot disabled = bot("disabled_bot", false);
        when(bots.findAll()).thenReturn(List.of(enabled, disabled));

        ManagedBotRegistry registry = new ManagedBotRegistry(bots);
        registry.loadEnabledBots();

        assertThat(registry.findEnabled("@HELPER_BOT")).contains(enabled);
        verify(bots, never()).findByUsername("helper_bot");
    }

    @Test
    void findEnabledLoadsFromDbAndCachesIfEnabled() {
        BotRepository bots = mock(BotRepository.class);
        ManagedBot enabled = bot("helper_bot", true);
        when(bots.findByUsername("helper_bot")).thenReturn(Optional.of(enabled));

        ManagedBotRegistry registry = new ManagedBotRegistry(bots);

        // First lookup: calls DB and caches
        Optional<ManagedBot> result = registry.findEnabled("HELPER_BOT");
        assertThat(result).contains(enabled);
        verify(bots, times(1)).findByUsername("helper_bot");

        // Second lookup: hits cache, does not call DB
        Optional<ManagedBot> cachedResult = registry.findEnabled("HELPER_BOT");
        assertThat(cachedResult).contains(enabled);
        verify(bots, times(1)).findByUsername("helper_bot");
    }

    @Test
    void putAndEvictUpdateRegistryState() {
        BotRepository bots = mock(BotRepository.class);
        ManagedBotRegistry registry = new ManagedBotRegistry(bots);
        ManagedBot bot = bot("temp_bot", true);

        registry.put(bot);
        assertThat(registry.findEnabled("temp_bot")).contains(bot);

        registry.evict("temp_bot");
        assertThat(registry.findEnabled("temp_bot")).isEmpty();
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
