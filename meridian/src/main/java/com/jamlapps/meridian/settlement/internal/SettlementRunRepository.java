package com.jamlapps.meridian.settlement.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SettlementRunRepository extends JpaRepository<SettlementRunEntity, UUID> {}
