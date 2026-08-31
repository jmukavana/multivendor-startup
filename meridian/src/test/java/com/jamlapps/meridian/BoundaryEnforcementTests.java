package com.jamlapps.meridian;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.Violations;

/**
 * AC-NFR-03: given a module boundary violation is introduced, when the build pipeline runs, then
 * the build fails.
 *
 * <p>{@code ModularityTests} proves the real modules are clean, which is a different claim: a
 * verification that never detects anything also passes. This test drives verification against a
 * fixture that is deliberately broken, so a regression that quietly disables enforcement — an
 * upgrade changing a default, someone removing {@code allowedDependencies} — fails the build here
 * rather than being discovered by a boundary violation reaching production.
 *
 * <p>The fixture lives in {@code com.jamlapps.archfixture} and must stay broken.
 */
class BoundaryEnforcementTests {

    private static final String FIXTURE = "com.jamlapps.archfixture";

    @Test
    @DisplayName("verification rejects a module reaching outside its allowed dependencies")
    void detectsDisallowedDependency() {
        // The fixture is compiled to the test classpath, which ArchUnit's default import option
        // excludes. Include every location so the deliberately broken modules are actually seen.
        ApplicationModules fixture = ApplicationModules.of(FIXTURE, (ImportOption) location -> true);

        assertThatExceptionOfType(Violations.class)
                .isThrownBy(fixture::verify)
                .withMessageContaining("beta")
                .withMessageContaining("alpha");
    }

    @Test
    @DisplayName("every application module constrains its dependencies")
    void everyModuleDeclaresAllowedDependencies() throws Exception {
        for (ApplicationModule module : ModularityTests.MODULES) {
            if (module.isOpen()) {
                continue;
            }

            String packageName = module.getBasePackage().getName();
            var annotation = Class.forName(packageName + ".package-info")
                    .getAnnotation(org.springframework.modulith.ApplicationModule.class);

            assertThat(annotation)
                    .as("Package '%s' has no @ApplicationModule annotation", packageName)
                    .isNotNull();

            assertThat(annotation.allowedDependencies())
                    .as("Module '%s' does not restrict its dependencies. Declare allowedDependencies "
                            + "in its package-info.java — an empty array if it should depend on nothing.",
                            module.getIdentifier())
                    .doesNotContain(org.springframework.modulith.ApplicationModule.OPEN_TOKEN);
        }
    }
}
