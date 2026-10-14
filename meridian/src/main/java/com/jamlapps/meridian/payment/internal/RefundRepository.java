package com.jamlapps.meridian.payment.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface RefundRepository extends JpaRepository<RefundEntity, UUID> {}
