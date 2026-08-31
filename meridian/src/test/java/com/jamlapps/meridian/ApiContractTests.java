package com.jamlapps.meridian;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jamlapps.meridian.web.ApiException;
import com.jamlapps.meridian.web.ApiResponse;
import com.jamlapps.meridian.web.ErrorCode;
import com.jamlapps.meridian.web.SortSpec;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Verifies the wire format the frontend already parses.
 *
 * <p>These assertions are deliberately about JSON rather than Java types. The frontend consumes
 * this over HTTP, so a rename that compiles and a serialisation setting that changes a field name
 * both break it in exactly the same way — invisibly, at runtime. The reference is
 * {@code meridian-frontend/src/shared/api/types.ts} and {@code error.ts}; if a test here changes,
 * that code changes with it.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import({TestcontainersConfiguration.class, ApiContractTests.ContractFixtureController.class})
class ApiContractTests {

    @Autowired
    MockMvc mvc;

    @Nested
    @DisplayName("success envelope")
    class SuccessEnvelope {

        @Test
        @DisplayName("wraps the payload in data and stamps meta")
        void wrapsPayload() throws Exception {
            mvc.perform(get("/api/contract-fixture/single"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("probe"))
                    .andExpect(jsonPath("$.meta").exists())
                    // ISO-8601 instant, not an epoch number. The frontend types generatedAt as a
                    // string and would render "1756640400.123" verbatim.
                    .andExpect(jsonPath("$.meta.generatedAt").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T.*Z?")))
                    // No field beyond the envelope: a stray top-level key means something is
                    // serialising itself outside the contract.
                    .andExpect(jsonPath("$.*", hasSize(2)));
        }

        @Test
        @DisplayName("collection responses carry pagination, sort and applied filters")
        void wrapsCollection() throws Exception {
            mvc.perform(get("/api/contract-fixture/page"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    // 1-based for display, converted from Spring Data's 0-based index.
                    .andExpect(jsonPath("$.meta.pagination.page").value(2))
                    .andExpect(jsonPath("$.meta.pagination.perPage").value(20))
                    .andExpect(jsonPath("$.meta.pagination.totalItems").value(137))
                    .andExpect(jsonPath("$.meta.pagination.totalPages").value(7))
                    .andExpect(jsonPath("$.meta.pagination.from").value(21))
                    .andExpect(jsonPath("$.meta.pagination.to").value(40))
                    .andExpect(jsonPath("$.meta.sort[0].field").value("createdAt"))
                    .andExpect(jsonPath("$.meta.sort[0].direction").value("desc"))
                    .andExpect(jsonPath("$.meta.appliedFilters.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.meta.generatedAt").exists());
        }
    }

    @Nested
    @DisplayName("problem details")
    class Problems {

        @Test
        @DisplayName("a thrown ApiException carries its code and status")
        void carriesCode() throws Exception {
            mvc.perform(get("/api/contract-fixture/not-found"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.name()))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.title").exists())
                    .andExpect(jsonPath("$.detail").value(containsString("Product 42")))
                    .andExpect(jsonPath("$.timestamp").exists())
                    // Typed as a required string on the frontend, so it must be present even when
                    // nothing is tracing.
                    .andExpect(jsonPath("$.traceId").exists());
        }

        @Test
        @DisplayName("bean validation failures are reported per field")
        void reportsFieldErrors() throws Exception {
            mvc.perform(post("/api/contract-fixture/validated")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                    .andExpect(jsonPath("$.fieldErrors.name").isArray())
                    .andExpect(jsonPath("$.fieldErrors.name[0]").exists());
        }

        @Test
        @DisplayName("an unhandled exception discloses nothing")
        void hidesInternalDetail() throws Exception {
            mvc.perform(get("/api/contract-fixture/boom"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_ERROR.name()))
                    // The original message names a table. It must not reach a client.
                    .andExpect(jsonPath("$.detail")
                            .value(allOf(not(containsString("vendor_payout_details")), not(containsString("SQL")))));
        }
    }

    @TestConfiguration
    @RestController
    @RequestMapping("/contract-fixture")
    static class ContractFixtureController {

        record Probe(String name) {}

        record ProbeRequest(@NotBlank(message = "must not be blank") String name) {}

        @GetMapping("/single")
        ApiResponse<Probe> single() {
            return ApiResponse.of(new Probe("probe"));
        }

        @GetMapping("/page")
        ApiResponse<List<Probe>> page() {
            return ApiResponse.paged(
                    List.of(new Probe("one"), new Probe("two")),
                    1,
                    20,
                    137,
                    List.of(SortSpec.desc("createdAt")),
                    Map.of("status", "ACTIVE"));
        }

        @GetMapping("/not-found")
        ApiResponse<Probe> notFound() {
            throw ApiException.notFound("Product", 42);
        }

        @PostMapping("/validated")
        ApiResponse<Probe> validated(@Valid @RequestBody ProbeRequest request) {
            return ApiResponse.of(new Probe(request.name()));
        }

        @GetMapping("/boom")
        ApiResponse<Probe> boom() {
            throw new IllegalStateException("SQL error: relation vendor_payout_details does not exist");
        }
    }
}
