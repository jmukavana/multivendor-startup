package com.jamlapps.meridian.order.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrderEventRepository extends JpaRepository<OrderEventEntity, UUID> {}
