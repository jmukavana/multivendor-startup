package com.jamlapps.meridian.order.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface ParentOrderRepository extends JpaRepository<ParentOrderEntity, UUID> {

    Optional<ParentOrderEntity> findByReference(String reference);

    /** BR-SEC-03: {@code Orders.findBySourceAddress}'s query. */
    List<ParentOrderEntity> findBySourceAddressOrderByCreatedAtDesc(String sourceAddress);

    /** BR-ORD-09: a buyer's order history, scoped to that buyer alone. */
    Page<ParentOrderEntity> findByBuyerAccountIdOrderByCreatedAtDesc(UUID buyerAccountId, Pageable pageable);

    /**
     * Multi-tenancy enforced in the WHERE clause: another buyer's order never leaves the database,
     * so it comes back exactly as if the id did not exist — see
     * {@code catalog.ProductRepository.findByIdAndVendorId}'s javadoc for the same reasoning
     * applied to vendor-scoped products.
     */
    Optional<ParentOrderEntity> findByIdAndBuyerAccountId(UUID id, UUID buyerAccountId);
}
