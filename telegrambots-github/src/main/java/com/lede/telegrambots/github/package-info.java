/**
 * GitHub integration: thin webhook controller, the webhook pipeline ({@code GitHubWebhookProcessor}),
 * HMAC-SHA256 signature verification, and event rendering (formatters). Hands rendered messages to
 * the {@code notification} module to broadcast.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "GitHub",
        allowedDependencies = {"bot", "notification", "shared", "mongo"})
package com.lede.telegrambots.github;
