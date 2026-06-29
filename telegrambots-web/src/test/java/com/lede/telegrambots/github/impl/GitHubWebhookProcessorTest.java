package com.lede.telegrambots.github.impl;

import com.lede.telegrambots.application.notification.BroadcastUseCase;
import com.lede.telegrambots.github.GitHubEventRenderer;
import com.lede.telegrambots.github.GitHubWebhookResult;
import com.lede.telegrambots.github.GitHubWebhookResult.Outcome;
import com.lede.telegrambots.github.GitHubWebhookStep;
import com.lede.telegrambots.github.steps.BotLookupStep;
import com.lede.telegrambots.github.steps.EventExecutionStep;
import com.lede.telegrambots.github.steps.EventPresenceStep;
import com.lede.telegrambots.github.steps.JsonParsingStep;
import com.lede.telegrambots.github.steps.PingCheckStep;
import com.lede.telegrambots.github.steps.RepositoryMatchStep;
import com.lede.telegrambots.github.steps.SignatureVerificationStep;
import com.lede.telegrambots.application.port.in.BotManagementUseCase;
import com.lede.telegrambots.application.port.out.WebhookSignatureVerifier;
import com.lede.telegrambots.domain.bot.ManagedBot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GitHubWebhookProcessor, proving the pipeline works correctly with
 * the Component-based Pipeline Pattern (GitHubWebhookStep).
 */
class GitHubWebhookProcessorTest {

    private BotManagementUseCase bots;
    private WebhookSignatureVerifier verifier;
    private GitHubEventRenderer renderer;
    private BroadcastUseCase notifications;
    private GitHubWebhookProcessor processor;

    private static final byte[] BODY =
            "{\"repository\":{\"full_name\":\"owner/repo\"}}".getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void setUp() {
        bots = mock(BotManagementUseCase.class);
        verifier = mock(WebhookSignatureVerifier.class);
        renderer = mock(GitHubEventRenderer.class);
        notifications = mock(BroadcastUseCase.class);
        when(renderer.render(eq("push"), any())).thenReturn(Optional.of("html"));

        // Build the pipeline manually to test in isolation
        List<GitHubWebhookStep> steps = List.of(
                new BotLookupStep(bots),
                new SignatureVerificationStep(verifier),
                new EventPresenceStep(),
                new PingCheckStep(),
                new JsonParsingStep(JsonMapper.builder().build()),
                new RepositoryMatchStep(),
                new EventExecutionStep(renderer, notifications)
        );

        processor = new GitHubWebhookProcessor(steps);
    }

    private static ManagedBot bot(String repo) {
        return new ManagedBot(null, "my_bot", "tok", null, repo, "ghsecret", true,
                Instant.now(), Instant.now());
    }

    @Test
    void unknownBotReturnsUnknownBot() {
        when(bots.findEnabled("ghost")).thenReturn(Optional.empty());

        GitHubWebhookResult r = processor.process("ghost", "push", "sig", "d1", BODY);

        assertEquals(Outcome.UNKNOWN_BOT, r.outcome());
        verify(renderer, never()).render(anyString(), any());
        verify(notifications, never()).broadcast(any(), anyString());
    }

    @Test
    void badSignatureReturnsBadSignature() {
        when(bots.findEnabled("my_bot")).thenReturn(Optional.of(bot("owner/repo")));
        when(verifier.verify(anyString(), any(), any())).thenReturn(false);

        GitHubWebhookResult r = processor.process("my_bot", "push", "sig", "d1", BODY);

        assertEquals(Outcome.BAD_SIGNATURE, r.outcome());
        verify(renderer, never()).render(anyString(), any());
        verify(notifications, never()).broadcast(any(), anyString());
    }

    @Test
    void pingReturnsPong() {
        when(bots.findEnabled("my_bot")).thenReturn(Optional.of(bot("owner/repo")));
        when(verifier.verify(any(), any(), any())).thenReturn(true);

        GitHubWebhookResult r = processor.process("my_bot", "ping", "sig", "d1", BODY);

        assertEquals(Outcome.PONG, r.outcome());
        verify(renderer, never()).render(anyString(), any());
        verify(notifications, never()).broadcast(any(), anyString());
    }

    @Test
    void repoMismatchIsIgnored() {
        when(bots.findEnabled("my_bot")).thenReturn(Optional.of(bot("owner/other")));
        when(verifier.verify(any(), any(), any())).thenReturn(true);

        GitHubWebhookResult r = processor.process("my_bot", "push", "sig", "d1", BODY);

        assertEquals(Outcome.REPO_MISMATCH, r.outcome());
        verify(renderer, never()).render(anyString(), any());
        verify(notifications, never()).broadcast(any(), anyString());
    }

    @Test
    void matchingRepoExecutesHandlerAndReturnsOk() {
        when(bots.findEnabled("my_bot")).thenReturn(Optional.of(bot("owner/repo")));
        when(verifier.verify(any(), any(), any())).thenReturn(true);

        GitHubWebhookResult r = processor.process("my_bot", "push", "sig", "d1", BODY);

        assertEquals(Outcome.OK, r.outcome());
        verify(renderer).render(eq("push"), any());
        verify(notifications).broadcast(any(ManagedBot.class), eq("html"));
    }
}
