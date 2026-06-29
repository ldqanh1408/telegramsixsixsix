/**
 * Admin REST API for bot CRUD, guarded by a constant-time admin token.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Admin API",
        allowedDependencies = {"bot", "config", "mongo"})
package com.lede.telegrambots.admin;
