/**
 * Bot management domain: the {@code BotManagementUseCase} port and its {@code DynamicBotManager}
 * facade, the bot registry, and registration command — coordinates activation on top.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Bot Management",
        allowedDependencies = {"activation", "shared", "mongo"})
package com.lede.telegrambots.bot;
