package com.jamlapps.meridian.integrity.internal;

import com.jamlapps.meridian.integrity.BuyerAbuseSignal;
import com.jamlapps.meridian.integrity.VendorFraudSignal;
import com.jamlapps.meridian.web.ApiResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** BR-SEC-04/05. */
@RestController
@RequestMapping("/admin/integrity")
class AdminAbuseController {

    private final AbuseDetectionService service;

    AdminAbuseController(AbuseDetectionService service) {
        this.service = service;
    }

    @GetMapping("/buyer-abuse")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<List<BuyerAbuseSignal>> buyerAbuse() {
        return ApiResponse.of(service.buyerAbuseSignals());
    }

    @GetMapping("/vendor-fraud")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<List<VendorFraudSignal>> vendorFraud() {
        return ApiResponse.of(service.vendorFraudSignals());
    }
}
