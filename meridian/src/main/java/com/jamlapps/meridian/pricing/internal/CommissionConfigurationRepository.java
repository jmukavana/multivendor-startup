package com.jamlapps.meridian.pricing.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface CommissionConfigurationRepository extends JpaRepository<CommissionConfigurationEntity, UUID> {

    @Query("select c from CommissionConfigurationEntity c where c.effectiveFrom <= current_timestamp "
            + "order by c.effectiveFrom desc limit 1")
    Optional<CommissionConfigurationEntity> findCurrent();
}
