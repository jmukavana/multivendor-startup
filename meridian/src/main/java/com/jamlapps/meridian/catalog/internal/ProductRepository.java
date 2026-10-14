package com.jamlapps.meridian.catalog.internal;

import com.jamlapps.meridian.catalog.ProductStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    /** BR-SCH-04: excludes non-active listings from the default view. */
    Page<ProductEntity> findByStatusOrderByIdDesc(ProductStatus status, Pageable pageable);

    /** BR-CAT-01: a vendor's own products, drafts and delisted included. */
    List<ProductEntity> findByVendorId(UUID vendorId);

    /**
     * BR-CAT-11, AC-CAT-01: multi-tenancy enforced in the WHERE clause, not by fetching the row
     * and checking it in Java. A product belonging to another vendor never leaves the database —
     * this returns empty exactly as if the id did not exist, which is what makes the service's
     * cross-vendor case indistinguishable from a genuine 404. A future query built on
     * {@code findById} alone would have no such guarantee and could leak another vendor's row the
     * moment someone forgot the check; a query built on this method cannot.
     */
    java.util.Optional<ProductEntity> findByIdAndVendorId(UUID id, UUID vendorId);

    /**
     * BR-VND-10: delists every active product for a suspended vendor in one statement. A bulk
     * update rather than loading each row: a vendor with a large catalog should not turn one
     * suspension into one update per product.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ProductEntity p set p.status = com.jamlapps.meridian.catalog.ProductStatus.DELISTED, "
            + "p.updatedAt = :now where p.vendorId = :vendorId and p.status = com.jamlapps.meridian.catalog.ProductStatus.ACTIVE")
    int delistAllForVendor(UUID vendorId, java.time.Instant now);

    /**
     * BR-SCH-01/02/04: {@code ProductSearchAdapter}'s query. Each optional filter is an {@code
     * (:param is null or ...)} clause rather than a dynamically-built query (a Specification or
     * QueryDSL) — four filters is few enough that the static form stays readable; a fifth would be
     * the point to reconsider.
     *
     * <p>{@code :keyword} is wrapped in {@code cast(... as string)} — without it, a null keyword
     * makes Postgres unable to infer the bind parameter's type from a bare {@code lower(?)} (it
     * defaults to {@code bytea} and {@code lower(bytea)} does not exist, failing with a real SQL
     * error on every keyword-less search, not just an edge case). The cast is a no-op for a
     * non-null value and gives Postgres an explicit type either way.
     */
    @Query("select p from ProductEntity p where p.status = com.jamlapps.meridian.catalog.ProductStatus.ACTIVE "
            + "and (:keyword is null or lower(p.name) like lower(concat('%', cast(:keyword as string), '%')) "
            + "or lower(p.description) like lower(concat('%', cast(:keyword as string), '%'))) "
            + "and (:minPriceMinorUnits is null or p.priceMinorUnits >= :minPriceMinorUnits) "
            + "and (:maxPriceMinorUnits is null or p.priceMinorUnits <= :maxPriceMinorUnits) "
            + "and (:vendorId is null or p.vendorId = :vendorId)")
    Page<ProductEntity> search(
            String keyword, Long minPriceMinorUnits, Long maxPriceMinorUnits, UUID vendorId, Pageable pageable);

    /**
     * BR-SCH-03's relevance sort: a genuine, if basic, signal — a name match outranks a
     * description-only match, ties broken by recency — not a recency sort renamed. Requires
     * {@code pageable} to carry no {@code Sort} of its own ({@code PageRequest.of(page, size)}):
     * this query's own {@code order by} is the ranking, and Spring Data would append a caller
     * supplied one after it rather than replace it.
     */
    @Query("select p from ProductEntity p where p.status = com.jamlapps.meridian.catalog.ProductStatus.ACTIVE "
            + "and (lower(p.name) like lower(concat('%', cast(:keyword as string), '%')) "
            + "or lower(p.description) like lower(concat('%', cast(:keyword as string), '%'))) "
            + "and (:minPriceMinorUnits is null or p.priceMinorUnits >= :minPriceMinorUnits) "
            + "and (:maxPriceMinorUnits is null or p.priceMinorUnits <= :maxPriceMinorUnits) "
            + "and (:vendorId is null or p.vendorId = :vendorId) "
            + "order by case when lower(p.name) like lower(concat('%', cast(:keyword as string), '%')) then 0 else 1 end, p.createdAt desc")
    Page<ProductEntity> searchByRelevance(
            String keyword, Long minPriceMinorUnits, Long maxPriceMinorUnits, UUID vendorId, Pageable pageable);
}
