package com.jamlapps.meridian.fulfilment.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface DeliveryRepository extends JpaRepository<DeliveryEntity, UUID> {

    Optional<DeliveryEntity> findBySubOrderId(UUID subOrderId);

    /** Repo-level vendor-ownership filtering, matching {@code order.internal.SubOrderRepository.findByIdAndVendorId}. */
    Optional<DeliveryEntity> findByIdAndVendorId(UUID id, UUID vendorId);

    /** BR-SEC-05: {@code VendorDispatchActivity.dispatchStats}'s query — every vendor with at least one delivery. */
    @Query("select new com.jamlapps.meridian.fulfilment.internal.VendorDispatchRow(d.vendorId, count(d), "
            + "sum(case when d.status = com.jamlapps.meridian.fulfilment.DeliveryStatus.FAILED then 1L else 0L end)) "
            + "from DeliveryEntity d group by d.vendorId")
    List<VendorDispatchRow> dispatchStatsByVendor();
}
