package com.jamlapps.meridian.vendor.internal;

import com.jamlapps.meridian.web.ApiResponse;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The public half of a vendor's identity: its display name. A product feed or product page shows
 * "sold by X" next to every listing, and {@code catalog} deliberately does not carry the vendor's
 * business name itself (it depends on {@code vendor}'s public API, not its entities — see {@code
 * Product}'s javadoc) — so the frontend resolves the names it needs here, in one batched call
 * rather than one request per product on the page.
 *
 * <p>Public like product listing and detail are (BR-IAM-08) — see {@code SecurityConfiguration}'s
 * permitAll for this exact path. Unknown or non-active vendor ids are silently dropped rather than
 * erroring: a page rendering twenty product cards should not fail whole because one vendor was
 * suspended between the catalog read and this one.
 */
@RestController
@RequestMapping("/vendors")
class VendorDirectoryController {

    /** Generous enough for any real product grid; a caller asking for more is asking wrong. */
    private static final int MAX_IDS = 100;

    private final VendorRepository vendors;

    VendorDirectoryController(VendorRepository vendors) {
        this.vendors = vendors;
    }

    record VendorSummary(UUID id, String businessName) {}

    @GetMapping
    ApiResponse<List<VendorSummary>> byIds(@RequestParam List<UUID> ids) {
        Set<UUID> requested = ids.stream().distinct().limit(MAX_IDS).collect(Collectors.toSet());

        List<VendorSummary> summaries = vendors.findAllById(requested).stream()
                .map(VendorEntity::toVendor)
                .map(vendor -> new VendorSummary(vendor.id(), vendor.businessName()))
                .toList();

        return ApiResponse.of(summaries);
    }
}
