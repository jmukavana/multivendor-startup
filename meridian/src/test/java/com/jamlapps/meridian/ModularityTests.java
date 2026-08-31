package com.jamlapps.meridian;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Enforces NFR-14: module boundaries are verified automatically in the build pipeline.
 *
 * <p>This test fails the build when a module reaches into another module's internals, when a
 * dependency appears that the target module's {@code package-info.java} does not permit, or when a
 * cycle forms between modules. It is the mechanism behind ADR-002; without it the dependency graph
 * declared there is documentation rather than architecture.
 */
class ModularityTests {

    static final ApplicationModules MODULES = ApplicationModules.of(MeridianApplication.class);

    @Test
    void verifiesModuleStructure() {
        MODULES.verify();
    }

    @Test
    void writesModuleDocumentation() {
        new Documenter(MODULES).writeDocumentation();
    }
}
