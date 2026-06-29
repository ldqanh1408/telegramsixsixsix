/**
 * Shared kernel: framework-agnostic value objects and utilities reusable by any module
 * ({@code BotUsername}, {@code MessageFormatter}). Depends on nothing.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Shared Kernel",
        allowedDependencies = {})
package com.lede.telegrambots.shared;
