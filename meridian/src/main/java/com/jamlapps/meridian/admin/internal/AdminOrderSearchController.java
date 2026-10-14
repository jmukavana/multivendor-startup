package com.jamlapps.meridian.admin.internal;

import com.jamlapps.meridian.order.ParentOrder;
import com.jamlapps.meridian.web.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** BR-ADM-02, BR-SEC-03. */
@RestController
@RequestMapping("/admin/orders/search")
class AdminOrderSearchController {

    private final AdminOrderSearchService service;

    AdminOrderSearchController(AdminOrderSearchService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'FINANCE_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<List<ParentOrder>> search(
            @RequestParam(required = false) @Nullable String reference,
            @RequestParam(required = false) @Nullable UUID buyerAccountId,
            @RequestParam(required = false) @Nullable String paymentReference,
            @RequestParam(required = false) @Nullable String sourceAddress) {
        return ApiResponse.of(service.search(reference, buyerAccountId, paymentReference, sourceAddress));
    }
}
