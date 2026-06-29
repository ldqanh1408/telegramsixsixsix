package com.lede.telegrambots.activation;

import com.lede.telegrambots.mongo.entity.GroupActivation;
import com.lede.telegrambots.mongo.entity.ManagedBot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GroupBotActivationServiceTest {

    @Test
    void activateReusesInactiveRecordAndMarksItActive() {
        ActivationStore store = mock(ActivationStore.class);
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
