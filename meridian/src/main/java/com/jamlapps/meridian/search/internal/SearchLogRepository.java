package com.jamlapps.meridian.search.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SearchLogRepository extends JpaRepository<SearchLogEntity, UUID> {}
