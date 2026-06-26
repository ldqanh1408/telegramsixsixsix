package com.lede.telegrambots.admin;

import com.lede.telegrambots.config.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

@Component
public class AdminAccessGuard {

    public static final String HEADER_NAME = "X-Admin-Token";

    private final AppProperties props;

    public AdminAccessGuard(AppProperties props) {
        this.props = props;
    }

    public Optional<AccessDenied> denyIfUnauthorized(String adminToken) {
        String expected = props.admin().token();
        if (expected == null || expected.isBlank()) {
            return Optional.of(new AccessDenied(HttpStatus.NOT_FOUND, "admin api disabled"));
        }
        if (!constantTimeEquals(expected, adminToken)) {
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
