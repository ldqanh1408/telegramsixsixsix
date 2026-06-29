package com.lede.telegrambots.application.activation;

import com.lede.telegrambots.application.port.out.ActivationRepository;
import com.lede.telegrambots.domain.activation.ActivationResult;
import com.lede.telegrambots.domain.activation.GroupActivation;
import com.lede.telegrambots.domain.bot.ManagedBot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupBotActivationServiceTest {

    @Test
    void activateReusesInactiveRecordAndMarksItActive() {
        ActivationRepository store = mock(ActivationRepository.class);
        ManagedBot bot = bot("helper_bot", true);
        Instant firstActivated = Instant.parse("2026-06-25T00:00:00Z");
        GroupActivation inactive = new GroupActivation(
                "activation-id",
                "helper_bot-id",
                "helper_bot",
                -100123L,
                false,
                firstActivated,
                firstActivated
        );
        when(store.find("helper_bot", -100123L)).thenReturn(Optional.of(inactive));
        when(store.save(any(GroupActivation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GroupBotActivationService service = new GroupBotActivationService(store);
        ActivationResult result = service.activate(bot, -100123L);

        assertThat(result.newlyActivated()).isTrue();
        assertThat(result.activation().id()).isEqualTo("activation-id");
        assertThat(result.activation().active()).isTrue();
        assertThat(result.activation().activatedAt()).isEqualTo(firstActivated);
    }

    @Test
    void activateRequestedIgnoresCommandsForDifferentBot() {
        ActivationRepository store = mock(ActivationRepository.class);
        ManagedBot bot = bot("helper_bot", true);

        GroupBotActivationService service = new GroupBotActivationService(store);
        GroupActivationCommandResult result = service.activateRequested(bot, -100123L, "@other_bot");

        assertThat(result.status()).isEqualTo(GroupActivationCommandResult.Status.BOT_MISMATCH);
        verify(store, never()).save(any());
    }

    @Test
    void activateRequestedReturnsAlreadyActiveWhenRecordIsActive() {
        ActivationRepository store = mock(ActivationRepository.class);
        ManagedBot bot = bot("helper_bot", true);
        Instant firstActivated = Instant.parse("2026-06-25T00:00:00Z");
        GroupActivation active = new GroupActivation(
                "activation-id",
                "helper_bot-id",
                "helper_bot",
                -100123L,
                true,
                firstActivated,
                firstActivated
        );
        when(store.find("helper_bot", -100123L)).thenReturn(Optional.of(active));

        GroupBotActivationService service = new GroupBotActivationService(store);
        GroupActivationCommandResult result = service.activateRequested(bot, -100123L, "@helper_bot");

        assertThat(result.status()).isEqualTo(GroupActivationCommandResult.Status.ALREADY_ACTIVE);
        assertThat(result.activation().newlyActivated()).isFalse();
    }

    @Test
    void deactivateRequestedReturnsNotActiveWhenNothingWasActive() {
        ActivationRepository store = mock(ActivationRepository.class);
        ManagedBot bot = bot("helper_bot", true);

        GroupBotActivationService service = new GroupBotActivationService(store);
        GroupActivationCommandResult result = service.deactivateRequested(bot, -100123L, "helper_bot");

        assertThat(result.status()).isEqualTo(GroupActivationCommandResult.Status.NOT_ACTIVE);
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
