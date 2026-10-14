package com.jamlapps.meridian.vendor.internal;

import com.jamlapps.meridian.vendor.VendorApplicationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface VendorApplicationRepository extends JpaRepository<VendorApplicationEntity, UUID> {

    List<VendorApplicationEntity> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    /**
     * Multi-tenancy enforced in the WHERE clause: another applicant's application never leaves the
     * database — see {@code catalog.ProductRepository.findByIdAndVendorId}'s javadoc for the same
     * reasoning applied to vendor-scoped products.
     */
    Optional<VendorApplicationEntity> findByIdAndAccountId(UUID id, UUID accountId);

    /** BR-ADM-01: the approval queue. */
    Page<VendorApplicationEntity> findByStatusInOrderBySubmittedAtAsc(
            List<VendorApplicationStatus> statuses, Pageable pageable);

    boolean existsByKraPinAndStatusIn(String kraPin, List<VendorApplicationStatus> statuses);

    Optional<VendorApplicationEntity> findByVendorId(UUID vendorId);
}
