package com.jamlapps.meridian.search.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** BR-SCH-06. */
@Entity
@Table(name = "search_log")
class SearchLogEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String keyword;

    @Column(name = "searched_at", nullable = false)
    private Instant searchedAt;

    protected SearchLogEntity() {
        // for JPA
    }

    SearchLogEntity(UUID id, String keyword, Instant searchedAt) {
        this.id = id;
        this.keyword = keyword;
        this.searchedAt = searchedAt;
    }
}
