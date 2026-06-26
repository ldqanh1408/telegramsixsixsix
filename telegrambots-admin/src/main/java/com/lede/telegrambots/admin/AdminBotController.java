package com.lede.telegrambots.admin;

import com.lede.telegrambots.admin.dto.AdminBotRequest;
import com.lede.telegrambots.admin.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

@RestController
@RequestMapping("/admin/bots")
public class AdminBotController {

    private final AdminAccessGuard access;
    private final AdminBotService service;

    public AdminBotController(AdminAccessGuard access, AdminBotService service) {
        this.access = access;
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestHeader(value = AdminAccessGuard.HEADER_NAME, required = false) String adminToken) {
        return authorized(adminToken, () -> ResponseEntity.ok(service.list()));
    }

    @GetMapping("/{username}")
    public ResponseEntity<?> get(
            @PathVariable String username,
            @RequestHeader(value = AdminAccessGuard.HEADER_NAME, required = false) String adminToken) {
        return authorized(adminToken, () -> service.find(username)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError("bot not found"))));
    }

    @PostMapping
    public ResponseEntity<?> createOrUpdate(
            @RequestBody AdminBotRequest request,
            @RequestHeader(value = AdminAccessGuard.HEADER_NAME, required = false) String adminToken) {
        return authorized(adminToken, () -> save(null, request));
    }

    @PutMapping("/{username}")
    public ResponseEntity<?> update(
            @PathVariable String username,
            @RequestBody AdminBotRequest request,
            @RequestHeader(value = AdminAccessGuard.HEADER_NAME, required = false) String adminToken) {
        return authorized(adminToken, () -> save(username, request));
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<?> delete(
            @PathVariable String username,
            @RequestHeader(value = AdminAccessGuard.HEADER_NAME, required = false) String adminToken) {
        return authorized(adminToken, () -> {
            service.delete(username);
            return ResponseEntity.noContent().build();
        });
    }

    private ResponseEntity<?> save(String usernameOverride, AdminBotRequest request) {
        try {
            return ResponseEntity.ok(service.save(usernameOverride, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        }
    }

    private ResponseEntity<?> authorized(String adminToken, Supplier<ResponseEntity<?>> action) {
        return access.denyIfUnauthorized(adminToken)
                .<ResponseEntity<?>>map(denied -> ResponseEntity.status(denied.status())
                        .body(new ApiError(denied.message())))
                .orElseGet(action);
    }
}
