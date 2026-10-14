package com.jamlapps.meridian.fulfilment.internal;

import com.jamlapps.meridian.fulfilment.DeliveryMethod;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface DeliveryRateCardRepository extends JpaRepository<DeliveryRateCardEntity, UUID> {

    /** Matches {@code pricing.internal.CommissionConfigurationRepository.findCurrent} exactly. */
    @Query("select r from DeliveryRateCardEntity r where r.carrierId = :carrierId and r.zoneId = :zoneId and r.method = :method "
            + "and r.effectiveFrom <= current_timestamp order by r.effectiveFrom desc limit 1")
    Optional<DeliveryRateCardEntity> findCurrent(UUID carrierId, UUID zoneId, DeliveryMethod method);
}
