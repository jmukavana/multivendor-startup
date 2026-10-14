package com.jamlapps.meridian.admin.internal;

import com.jamlapps.meridian.admin.AuditLogEntry;
import com.jamlapps.meridian.web.ApiResponse;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** BR-ADM-07: the read side of the audit log — {@code web.AuditLog} (via {@link AuditLogService}) is the write side. */
@RestController
@RequestMapping("/admin/audit-log")
class AdminAuditController {

    private final AuditLogService service;

    AdminAuditController(AuditLogService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<List<AuditLogEntry>> list(
            @RequestParam(required = false) @Nullable String entityType, @RequestParam(required = false) @Nullable String entityId) {
        if (entityType != null && entityId != null) {
            return ApiResponse.of(service.forEntity(entityType, entityId));
        }
        return ApiResponse.of(service.all());
    }
}
