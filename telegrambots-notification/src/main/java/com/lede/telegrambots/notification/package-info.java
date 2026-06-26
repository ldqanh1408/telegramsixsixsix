/**
 * Source-agnostic broadcaster: delivers a rendered HTML message to every active group of a bot.
 * Knows nothing about GitHub — event rendering lives in the {@code github} module.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Notification",
        allowedDependencies = {"bot", "telegram", "mongo"})
package com.lede.telegrambots.notification;
