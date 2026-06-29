package com.lede.telegrambots.admin.impl;



import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAccessGuardTest {

    @Test
    void deniesAccessWhenAdminTokenIsNotConfigured() {
        AdminAccessGuard guard = new AdminAccessGuard("");

        assertThat(guard.denyIfUnauthorized("anything"))
                .contains(new AdminAccessGuard.AccessDenied(HttpStatus.NOT_FOUND, "admin api disabled"));
    }

    @Test
    void deniesAccessWhenTokenDoesNotMatch() {
        AdminAccessGuard guard = new AdminAccessGuard("secret");

        assertThat(guard.denyIfUnauthorized("wrong"))
                .contains(new AdminAccessGuard.AccessDenied(HttpStatus.UNAUTHORIZED, "unauthorized"));
    }

    @Test
    void allowsAccessWhenTokenMatches() {
        AdminAccessGuard guard = new AdminAccessGuard("secret");

        assertThat(guard.denyIfUnauthorized("secret")).isEmpty();
    }
}
