/**
 * Persistence kernel: MongoDB entity records ({@code ManagedBot}, {@code GroupActivation})
 * and Spring Data repositories.
 *
 * <p>Declared {@code OPEN} so its entity/repository types — the project's shared persistence
 * vocabulary — are visible to the feature modules that store and read them, without forcing
 * an anti-corruption layer for every record.</p>
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Persistence",
        type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.lede.telegrambots.mongo;
