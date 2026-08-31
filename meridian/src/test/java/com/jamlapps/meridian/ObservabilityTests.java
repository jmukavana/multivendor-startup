package com.jamlapps.meridian;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

/**
 * NFR-11: structured logging, distributed tracing and metrics in place from first deployment.
 *
 * <p>Observability is the kind of configuration that appears correct and does nothing. A missing
 * dependency, a renamed property or an autoconfiguration that backed off silently all present as a
 * healthy application with no traces. These assertions fail the build instead, which matters most
 * before Phase 3, when NFR-12 makes reconciliation variance and settlement failure priority
 * incidents that alerting has to detect.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ObservabilityTests {

    @Autowired
    ApplicationContext context;

    @Autowired
    MeterRegistry meterRegistry;

    @Autowired
    Tracer tracer;

    @Test
    @DisplayName("metrics registry is present and tagged for the environment")
    void metricsAreCollected() {
        meterRegistry.counter("meridian.observability.probe").increment();

        assertThat(meterRegistry.find("meridian.observability.probe").counter())
                .isNotNull()
                .satisfies(counter -> assertThat(counter.count()).isEqualTo(1.0d));

        assertThat(meterRegistry.getMeters())
                .as("common tags from management.metrics.tags should be applied")
                .anySatisfy(meter -> assertThat(meter.getId().getTag("application")).isEqualTo("meridian"));
    }

    @Test
    @DisplayName("a tracer is wired and produces a span with a trace id")
    void tracingIsWired() {
        var span = tracer.nextSpan().name("observability-probe").start();

        try (var ignored = tracer.withSpan(span)) {
            assertThat(tracer.currentSpan()).isNotNull();
            assertThat(span.context().traceId()).isNotBlank();
        } finally {
            span.end();
        }
    }

    @Test
    @DisplayName("modulith observability instruments module invocations")
    void modulithObservabilityIsRegistered() {
        assertThat(context.getBeanDefinitionNames())
                .as("spring-modulith-observability should contribute module tracing")
                .anySatisfy(name -> assertThat(name.toLowerCase()).contains("modulith"));
    }
}
