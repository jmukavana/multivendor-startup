package com.jamlapps.meridian.web.internal;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The API document at {@code /v3/api-docs}.
 *
 * <p>This is the artifact the frontend generates its request and response types from, which makes
 * it the contract rather than documentation. A hand-maintained parallel type definition on the
 * frontend is how the two drift, and the drift is only discovered at runtime.
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

    @Bean
    OpenAPI meridianOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Meridian API")
                        .version("v1")
                        .description(
                                """
                                Multi-vendor marketplace backend.

                                Every successful response is wrapped in `{ "data": ..., "meta": ... }`.
                                Errors are RFC 7807 problem details extended with `code`, `traceId`,
                                `timestamp` and, on validation failures, `fieldErrors`. Clients branch
                                on `code`, never on the message: messages are written for operators \
                                and are not a stable interface.
                                """)
                        .license(new License().name("Proprietary")))
                // Declared so generated clients default to the right prefix; every @RestController
                // is served under /api by WebConfiguration.
                .servers(List.of(new Server().url("/api").description("Default")));
    }
}
