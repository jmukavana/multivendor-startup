package com.jamlapps.meridian.notification.internal;

import com.jamlapps.meridian.web.ApiResponse;
import com.jamlapps.meridian.web.AuthenticatedCaller;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** BR-NTF-04: a buyer's own marketing opt-out — infrastructure ahead of use, see this module's package doc. */
@RestController
@RequestMapping("/notifications/preferences")
class NotificationPreferenceController {

    private final NotificationService service;

    NotificationPreferenceController(NotificationService service) {
        this.service = service;
    }

    record MarketingPreference(boolean marketingOptOut) {}

    @GetMapping
    ApiResponse<MarketingPreference> get(@AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(new MarketingPreference(service.marketingOptOut(principal.accountId())));
    }

    @PutMapping
    ApiResponse<MarketingPreference> set(@RequestBody MarketingPreference request, @AuthenticationPrincipal AuthenticatedCaller principal) {
        service.setMarketingOptOut(principal.accountId(), request.marketingOptOut());
        return ApiResponse.of(request);
    }
}
