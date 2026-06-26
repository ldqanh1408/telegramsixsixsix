package com.lede.telegrambots;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Enforces the modular architecture. {@link #verifiesModuleStructure()} fails the build if any
 * module reaches into another module's internals, depends on a module not in its
 * {@code allowedDependencies}, or introduces a dependency cycle.
 *
 * <p>{@link #writesDocumentation()} renders the module diagrams + canvases into
 * {@code target/spring-modulith-docs}.</p>
 */
class ModularityTests {

    static final ApplicationModules modules = ApplicationModules.of(TelegrambotsApplication.class);

    @Test
    void verifiesModuleStructure() {
        modules.verify();
    }

    @Test
    void writesDocumentation() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();
    }
}
