package com.lede.telegrambots.admin.impl;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

/**
 * Guards the admin API with a constant-time token comparison. The expected token is bound straight
 * from configuration ({@code app.admin.token}); a blank value disables the admin API entirely.
 */
@Component
public class AdminAccessGuard {

    public static final String HEADER_NAME = "X-Admin-Token";

    private final String expectedToken;

    public AdminAccessGuard(@Value("${app.admin.token:}") String expectedToken) {
        this.expectedToken = expectedToken == null ? "" : expectedToken;
    }

    public Optional<AccessDenied> denyIfUnauthorized(String adminToken) {
        if (expectedToken.isBlank()) {
            return Optional.of(new AccessDenied(HttpStatus.NOT_FOUND, "admin api disabled"));
        }
        if (!constantTimeEquals(expectedToken, adminToken)) {
            return Optional.of(new AccessDenied(HttpStatus.UNAUTHORIZED, "unauthorized"));
        }
        return Optional.empty();
    }

    private static boolean constantTimeEquals(String expected, String received) {
        if (received == null) return false;
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                received.getBytes(StandardCharsets.UTF_8)
        );
    }

    public record AccessDenied(HttpStatus status, String message) {
    }
}
