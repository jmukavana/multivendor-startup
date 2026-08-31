package com.jamlapps.meridian.iam.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AuthEventRepository extends JpaRepository<AuthEventEntity, UUID> {}
