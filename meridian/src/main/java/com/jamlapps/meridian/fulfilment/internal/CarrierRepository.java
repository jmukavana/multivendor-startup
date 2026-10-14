package com.jamlapps.meridian.fulfilment.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CarrierRepository extends JpaRepository<CarrierEntity, UUID> {

    List<CarrierEntity> findByActiveTrue();
}
