/**
 * Group activation domain: binds a bot to a Telegram chat (activate / deactivate / count).
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Activation",
        allowedDependencies = {"shared", "mongo"})
package com.lede.telegrambots.activation;
