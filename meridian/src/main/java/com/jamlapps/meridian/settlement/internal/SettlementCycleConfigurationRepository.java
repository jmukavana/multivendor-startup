package com.jamlapps.meridian.settlement.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SettlementCycleConfigurationRepository extends JpaRepository<SettlementCycleConfigurationEntity, UUID> {

    @Query("select c from SettlementCycleConfigurationEntity c where c.effectiveFrom <= current_timestamp "
            + "order by c.effectiveFrom desc limit 1")
    Optional<SettlementCycleConfigurationEntity> findCurrent();
}
