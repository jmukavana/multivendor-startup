package com.jamlapps.meridian.admin.internal;

import com.jamlapps.meridian.admin.AuditLogEntry;
import com.jamlapps.meridian.web.AuditLog;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-ADM-07: the real implementation behind {@code web.AuditLog}. {@link #record} runs {@code
 * REQUIRES_NEW} — deliberately, for the same reason {@code integrity.internal.RateLimitBreachRecorder}
 * and {@code inventory.internal.OversellAttemptRecorder} do: several of this module's real callers
 * (a rejection, a moderation removal) log the very thing they are about to fail or refuse on behalf
 * of, and an audit entry that vanished along with the transaction it is auditing would defeat the
 * requirement. A successful action's audit entry surviving that same guarantee is no worse for it.
 */
@Service
class AuditLogService implements AuditLog {

    private final AuditLogEntryRepository entries;
    private final Clock clock;

    AuditLogService(AuditLogEntryRepository entries, Clock clock) {
        this.entries = entries;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(@Nullable UUID actorAccountId, String action, String entityType, String entityId, @Nullable String detail) {
        entries.save(new AuditLogEntryEntity(UUID.randomUUID(), actorAccountId, action, entityType, entityId, detail, clock.instant()));
    }

    @Transactional(readOnly = true)
    List<AuditLogEntry> all() {
        return entries.findAllByOrderByOccurredAtDesc().stream().map(AuditLogEntryEntity::toAuditLogEntry).toList();
    }

    @Transactional(readOnly = true)
    List<AuditLogEntry> forEntity(String entityType, String entityId) {
        return entries.findByEntityTypeAndEntityIdOrderByOccurredAtDesc(entityType, entityId).stream()
                .map(AuditLogEntryEntity::toAuditLogEntry)
                .toList();
    }
}
