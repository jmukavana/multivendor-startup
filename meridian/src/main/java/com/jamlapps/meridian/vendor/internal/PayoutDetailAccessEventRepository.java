package com.jamlapps.meridian.vendor.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PayoutDetailAccessEventRepository extends JpaRepository<PayoutDetailAccessEventEntity, UUID> {}
