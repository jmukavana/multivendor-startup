package com.jamlapps.meridian.integrity.internal;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RateLimitBucketRepository extends JpaRepository<RateLimitBucketEntity, UUID> {

    /**
     * The atomic increment {@link RateLimiterService} depends on. JPQL has no upsert, so this is
     * the one native, Postgres-specific query in this codebase's repositories — every other
     * atomic write elsewhere ({@code inventory.internal.StockRepository.tryReserve}, say) is a
     * single conditional {@code UPDATE} because the row it updates always already exists; a rate
     * limit bucket's row does not exist until its window's first request, so the same technique
     * does not apply here. Two concurrent first-requests in a fresh window race at {@code ON
     * CONFLICT}, not in application code, the same guarantee {@code tryReserve}'s row-level lock
     * gives — only one statement's conflict branch runs the increment, the other bootstraps it.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = "insert into rate_limit_bucket (id, scope, rate_limit_key, window_start, count) "
                    + "values (:id, :scope, :key, :windowStart, 1) "
                    + "on conflict (scope, rate_limit_key, window_start) "
                    + "do update set count = rate_limit_bucket.count + 1",
            nativeQuery = true)
    void upsertIncrement(
            @Param("id") UUID id, @Param("scope") String scope, @Param("key") String key, @Param("windowStart") Instant windowStart);

    Optional<RateLimitBucketEntity> findByScopeAndKeyAndWindowStart(String scope, String key, Instant windowStart);
}
