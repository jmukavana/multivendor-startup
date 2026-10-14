package com.jamlapps.meridian.pricing.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface HoldbackConfigurationRepository extends JpaRepository<HoldbackConfigurationEntity, UUID> {

    @Query("select h from HoldbackConfigurationEntity h where h.effectiveFrom <= current_timestamp "
            + "order by h.effectiveFrom desc limit 1")
    Optional<HoldbackConfigurationEntity> findCurrent();
}
