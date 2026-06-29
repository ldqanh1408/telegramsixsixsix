/**
 * GitHub integration: thin webhook controller, the webhook pipeline ({@code GitHubWebhookProcessor}
 * built on the shared {@code application.pipeline} abstraction), and event rendering (formatters).
 * Hands rendered messages to the application's {@code BroadcastUseCase}.
 */
package com.lede.telegrambots.github;
