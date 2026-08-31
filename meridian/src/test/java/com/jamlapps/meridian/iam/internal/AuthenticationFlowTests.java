package com.jamlapps.meridian.iam.internal;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jamlapps.meridian.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * The BR-IAM-01/02/05 flow end to end: register, receive a code, verify, use the session, sign
 * out, and confirm the session is dead — through real HTTP, real hashing, and a real database.
 *
 * <p>The session token is read from the response body and sent back as {@code Authorization:
 * Bearer <token>}, matching how {@code meridian-frontend/src/shared/api/client.ts} actually talks
 * to this API and how {@code AuthController}'s class doc explains why — there is no browser cookie
 * for this backend to see, only the Next.js server's bearer header.
 *
 * <p>Lives inside {@code iam.internal} deliberately, alongside the code it tests, so it can use
 * {@link CapturingOneTimeCodeGenerator} to retrieve a plaintext code that {@code AuthenticationService}
 * never exposes any other way (see that generator's javadoc, and ADR-008). A test outside this
 * package has no such route, which is the point.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, CapturingOneTimeCodeGenerator.class})
class AuthenticationFlowTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    CapturingOneTimeCodeGenerator codes;

    @Test
    @DisplayName("register, verify, use the session, sign out — and the session is then rejected")
    void fullSessionLifecycle() throws Exception {
        String identifier = "+254700111222";

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"%s\",\"displayName\":\"Test Buyer\"}".formatted(identifier)))
                .andExpect(status().isOk());

        String code = codes.mostRecentlyGenerated();

        MvcResult verifyResult = mvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"%s\",\"code\":\"%s\"}".formatted(identifier, code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.expiresAt", notNullValue()))
                .andExpect(jsonPath("$.data.canCheckOut").value(true))
                .andReturn();

        String token = com.jayway.jsonpath.JsonPath.read(
                verifyResult.getResponse().getContentAsString(), "$.data.token");

        // The session is live: the protected endpoint accepts it and revokes every session for the
        // account, including the one making this call.
        mvc.perform(delete("/api/auth/sessions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Reusing the same token proves revocation under ADR-008 takes effect on the very next
        // request, not eventually — a stale token is not merely "will stop working soon".
        mvc.perform(delete("/api/auth/sessions").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SESSION_EXPIRED"));
    }

    @Test
    @DisplayName("a wrong code is rejected without revealing which part was wrong")
    void wrongCodeRejected() throws Exception {
        String identifier = "+254700111333";

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"%s\",\"displayName\":\"Test Buyer\"}".formatted(identifier)))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"%s\",\"code\":\"000000\"}".formatted(identifier)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.detail", notNullValue()));
    }

    @Test
    @DisplayName("requesting a code for an unregistered identifier looks identical to a registered one")
    void requestCodeDoesNotRevealExistence() throws Exception {
        mvc.perform(post("/api/auth/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"+254700999999\"}"))
                .andExpect(status().isOk())
                // Same 200 with an empty envelope either way — see AuthenticationService's class doc.
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("a protected endpoint without a session is rejected with SESSION_EXPIRED, not a generic 401")
    void protectedEndpointRequiresSession() throws Exception {
        mvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SESSION_EXPIRED"))
                .andExpect(jsonPath("$.traceId", notNullValue()));
    }
}
