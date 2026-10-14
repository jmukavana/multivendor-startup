package com.jamlapps.meridian;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.modulith.Modulithic;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@Modulithic(systemName = "Meridian", sharedModules = {"shared", "web"})
@EnableConfigurationProperties(com.jamlapps.meridian.shared.BlobStorageProperties.class)
// integrity.internal.RateLimitProperties is genuinely internal to that module (BR-SEC-01's
// configurable thresholds) — @EnableConfigurationProperties(Class) here would be a source-level
// reference from this root class straight into another module's internal package, exactly what
// ModularityTests exists to forbid. A package scan binds it by name instead, with no such edge.
@ConfigurationPropertiesScan("com.jamlapps.meridian.integrity.internal")
@EnableScheduling
public class MeridianApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeridianApplication.class, args);
    }

}
