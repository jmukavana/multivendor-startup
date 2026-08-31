package com.jamlapps.meridian.web.internal;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.method.HandlerTypePredicate;

@Configuration(proxyBeanMethods = false)
class WebConfiguration implements WebMvcConfigurer {

    /**
     * Every {@code @RestController} is served under {@code /api}, which is what
     * {@code MERIDIAN_API_URL} in the frontend points at.
     *
     * <p>Done as a path prefix rather than a servlet context path so that {@code /actuator} stays
     * where Kubernetes probes and the Prometheus scrape config expect it.
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api", HandlerTypePredicate.forAnnotation(RestController.class));
    }

    /**
     * A single injectable clock. Response timestamps are asserted in tests, and a fixed clock is
     * the difference between asserting a value and asserting that a value exists.
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
