package com.jamlapps.meridian.catalog.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

/**
 * BR-CAT-08, AC-CAT-03. The seed rows in V8 are placeholders — OPEN-07 records that the real
 * prohibited-items list has not been drafted by Legal. This makes the CHECK mechanism real and
 * testable ahead of that list existing; the actual terms are a business decision.
 */
interface ProhibitedTermRepository extends JpaRepository<ProhibitedTermEntity, UUID> {}
