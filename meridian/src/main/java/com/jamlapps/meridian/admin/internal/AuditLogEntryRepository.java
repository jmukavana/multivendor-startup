package com.jamlapps.meridian.admin.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AuditLogEntryRepository extends JpaRepository<AuditLogEntryEntity, UUID> {

    List<AuditLogEntryEntity> findAllByOrderByOccurredAtDesc();

    List<AuditLogEntryEntity> findByEntityTypeAndEntityIdOrderByOccurredAtDesc(String entityType, String entityId);
}
