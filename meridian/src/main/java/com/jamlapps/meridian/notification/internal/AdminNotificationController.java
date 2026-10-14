package com.jamlapps.meridian.notification.internal;

import com.jamlapps.meridian.notification.NotificationMessage;
import com.jamlapps.meridian.web.ApiResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * BR-NTF-05's admin exception queue, plus a delivery-history lookup for ops (and for tests, which
 * otherwise have no way to observe a SENT message — the same testability gap {@code
 * tax.internal.TaxService.getInvoiceByOrder} was added to close). Lives in {@code
 * notification.internal}, method-secured, matching every other admin surface this codebase has
 * built ahead of an {@code admin} aggregation module existing.
 */
@RestController
@RequestMapping("/admin/notifications")
class AdminNotificationController {

    private final NotificationService service;

    AdminNotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping("/exceptions")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<List<NotificationMessage>> exceptions() {
        return ApiResponse.of(service.exceptionMessages());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<List<NotificationMessage>> forRecipient(@RequestParam String recipient) {
        return ApiResponse.of(service.forRecipient(recipient));
    }
}
