/**
 * Telegram integration: webhook controller, {@code TelegramSender} port + {@code TelegramClient}
 * adapter, command router and pure command handlers.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Telegram",
        allowedDependencies = {"bot", "activation", "shared", "mongo"})
package com.lede.telegrambots.telegram;
