package com.jamlapps.meridian.vendor.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface VerificationDocumentRepository extends JpaRepository<VerificationDocumentEntity, UUID> {

    List<VerificationDocumentEntity> findByApplicationId(UUID applicationId);
}
