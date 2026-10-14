package com.jamlapps.meridian.catalog.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "prohibited_term")
class ProhibitedTermEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String term;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    protected ProhibitedTermEntity() {
        // for JPA
    }

    String term() {
        return term;
    }
}
