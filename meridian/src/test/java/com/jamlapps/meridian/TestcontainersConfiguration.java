package com.jamlapps.meridian;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /**
     * A static field, not an instance one returned fresh each time: Spring creates a new
     * {@code TestcontainersConfiguration} per {@code ApplicationContext}, and this codebase's test
     * classes create several distinct contexts — different {@code @Import} combinations hash
     * differently, so Spring's context cache cannot treat them as one. Each of those contexts
     * starting its own PostgreSQL container is what made a full {@code ./gradlew build} reliably
     * fail under memory pressure: several containers alive simultaneously on a machine with little
     * free RAM, rather than a bug in any test. One container, started once and reused as the
     * {@code @ServiceConnection} bean by every context in the JVM, removes that failure mode
     * outright rather than relying on the machine having enough headroom on a given run.
     *
     * <p>Tests already tolerate a shared, non-rolled-back database across methods within one class
     * (see the presence-over-count assertions throughout {@code WalkingSkeletonFlowTests}), so
     * sharing it across classes too introduces no new isolation problem — each class already seeds
     * data with identifiers unlikely to collide (distinct phone number ranges, random UUIDs).
     * Testcontainers' Ryuk reaper still tears the container down when the JVM exits.
     */
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));

    static {
        POSTGRES.start();
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return POSTGRES;
    }
}
