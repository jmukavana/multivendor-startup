package com.jamlapps.meridian.catalog.internal;

import com.jamlapps.meridian.catalog.Product;
import com.jamlapps.meridian.catalog.ProductStatus;
import com.jamlapps.meridian.shared.BlobStorage;
import com.jamlapps.meridian.shared.Money;
import com.jamlapps.meridian.vendor.Vendors;
import com.jamlapps.meridian.web.ApiException;
import com.jamlapps.meridian.web.AuditLog;
import com.jamlapps.meridian.web.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

    private final ProductRepository products;
    private final ProductImageRepository images;
    private final ProductPriceChangeRepository priceChanges;
    private final Vendors vendors;
    private final ProhibitedTermChecker prohibitedTerms;
    private final BlobStorage blobStorage;
    private final AuditLog auditLog;
    private final Clock clock;

    CatalogService(
            ProductRepository products,
            ProductImageRepository images,
            ProductPriceChangeRepository priceChanges,
            Vendors vendors,
            ProhibitedTermChecker prohibitedTerms,
            BlobStorage blobStorage,
            AuditLog auditLog,
            Clock clock) {
        this.products = products;
        this.images = images;
        this.priceChanges = priceChanges;
        this.vendors = vendors;
        this.prohibitedTerms = prohibitedTerms;
        this.blobStorage = blobStorage;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    /**
     * BR-CAT-01/02, AC-VND-02: only an approved, active vendor may create a product, scoped to
     * exactly that vendor. A vendor row only exists once an application is approved (see
     * {@code Vendor}'s javadoc), so "no vendor row" already refuses every unapproved state; the
     * check here adds the one live-row state AC-VND-02 also requires refused: suspended.
     *
     * <p>Created DRAFT, not ACTIVE — see {@link #publish}.
     */
    @Transactional
    Product create(UUID callerAccountId, String name, @Nullable String description, Money price) {
        var vendor = vendors
                .findByOwnerAccountId(callerAccountId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.FORBIDDEN, "Only a vendor may list products. Create a vendor first."));

        if (!vendor.isActive()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "A suspended vendor may not list products.");
        }

        ProductEntity entity = products.save(
                new ProductEntity(UUID.randomUUID(), vendor.id(), name, description, price, clock.instant()));
        return entity.toProduct();
    }

    /**
     * A delisted or draft product 404s for a public read — it was never meant to be visible by
     * direct link once removed or before publication, matching the exclusion already applied to
     * the listing (BR-SCH-04).
     */
    @Transactional(readOnly = true)
    Product get(UUID productId) {
        ProductEntity entity =
                products.findById(productId).orElseThrow(() -> ApiException.notFound("Product", productId));

        if (entity.status() != ProductStatus.ACTIVE) {
            throw ApiException.notFound("Product", productId);
        }

        return entity.toProduct();
    }

    @Transactional(readOnly = true)
    Page<Product> list(Pageable pageable) {
        return products.findByStatusOrderByIdDesc(ProductStatus.ACTIVE, pageable).map(ProductEntity::toProduct);
    }

    /** BR-CAT-01, AC-CAT-01: a vendor's own products, including drafts, buyers never see. */
    @Transactional(readOnly = true)
    List<Product> listMine(UUID callerAccountId) {
        UUID vendorId = requireVendorId(callerAccountId);
        return products.findByVendorId(vendorId).stream().map(ProductEntity::toProduct).toList();
    }

    /**
     * AC-CAT-01, BR-CAT-11: a product owned by another vendor is refused as not-found, and the
     * attempt logged. The isolation itself is enforced by {@code findByIdAndVendorId}'s WHERE
     * clause, not by fetching the row and checking it here — see that method's javadoc. The log
     * line is best-effort visibility on top of that, not the mechanism the isolation depends on;
     * see this method's note in the module's "not yet done" list for the persistence gap that
     * leaves if the log itself is lost.
     */
    @Transactional(readOnly = true)
    Product getMine(UUID productId, UUID callerAccountId) {
        UUID vendorId = requireVendorId(callerAccountId);
        return products
                .findByIdAndVendorId(productId, vendorId)
                .map(ProductEntity::toProduct)
                .orElseGet(() -> {
                    logCrossVendorAttempt(productId, callerAccountId, vendorId);
                    throw ApiException.notFound("Product", productId);
                });
    }

    /** BR-CAT-05, BR-CAT-08, AC-CAT-02, AC-CAT-03: DRAFT -> ACTIVE. */
    @Transactional
    Product publish(UUID productId, UUID callerAccountId) {
        ProductEntity entity = requireOwned(productId, callerAccountId);

        if (images.countByProductId(productId) == 0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "A product needs at least one image before it can publish.");
        }

        var match = prohibitedTerms.firstMatch(entity.name());
        if (match.isPresent()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR, "This listing matches a prohibited term: \"" + match.get() + "\".");
        }

        entity.publish(clock.instant());
        return entity.toProduct();
    }

    @Transactional
    Product delist(UUID productId, UUID callerAccountId) {
        ProductEntity entity = requireOwned(productId, callerAccountId);
        entity.delist(clock.instant());
        return entity.toProduct();
    }

    /**
     * BR-ADM-05: an admin-initiated removal for a content-policy violation — deliberately not
     * {@link #delist}, which is ownership-scoped to the vendor's own ACTIVE product. This bypasses
     * that ownership check by design (an admin is not the product's owner) and works from any
     * status, not only ACTIVE, since a policy violation can be found on a DRAFT listing too.
     */
    @Transactional
    Product moderateDelist(UUID productId, UUID adminAccountId, String reason) {
        ProductEntity entity = products.findById(productId).orElseThrow(() -> ApiException.notFound("Product", productId));
        entity.delist(clock.instant());

        // REQUIRES_NEW inside AuditLogService — survives regardless of what this transaction does next.
        auditLog.record(adminAccountId, "PRODUCT_MODERATED", "Product", productId.toString(), reason);

        return entity.toProduct();
    }

    /** BR-CAT-09, AC-CAT-04: records the change so it is retrievable — see {@link #priceHistory}. */
    @Transactional
    Product changePrice(UUID productId, UUID callerAccountId, Money newPrice) {
        ProductEntity entity = requireOwned(productId, callerAccountId);
        Instant now = clock.instant();
        Money previousPrice = entity.changePrice(newPrice, now);

        priceChanges.save(new ProductPriceChangeEntity(
                UUID.randomUUID(), productId, previousPrice, newPrice, callerAccountId, now));

        return entity.toProduct();
    }

    @Transactional(readOnly = true)
    List<ProductPriceChangeEntity.PriceChangeView> priceHistory(UUID productId, UUID callerAccountId) {
        requireOwned(productId, callerAccountId);
        return priceChanges.findByProductIdOrderByChangedAtDesc(productId).stream()
                .map(ProductPriceChangeEntity::toView)
                .toList();
    }

    @Transactional
    void addImage(UUID productId, UUID callerAccountId, InputStream content, String contentType, long size) {
        requireOwned(productId, callerAccountId);

        String reference;
        try (content) {
            reference = blobStorage.store(content, contentType, size);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        int nextOrder = (int) images.countByProductId(productId);
        images.save(new ProductImageEntity(UUID.randomUUID(), productId, reference, contentType, nextOrder, clock.instant()));
    }

    /**
     * BR-CAT-05's missing other half: images could be uploaded but never read back. Same
     * visibility as the product itself — {@link #get} already 404s a draft or delisted product,
     * and a gallery for a product a buyer cannot see would leak that it exists.
     */
    @Transactional(readOnly = true)
    List<ProductImageEntity> listImages(UUID productId) {
        get(productId);
        return images.findByProductId(productId);
    }

    /**
     * One image's bytes. Ownership of {@code imageId} by {@code productId} is checked here, not
     * assumed from the URL — a guessed or stale image id for a different product must not leak
     * that product's picture.
     */
    @Transactional(readOnly = true)
    ProductImageEntity image(UUID productId, UUID imageId) {
        get(productId);
        return images.findById(imageId)
                .filter(image -> image.productId().equals(productId))
                .orElseThrow(() -> ApiException.notFound("Product image", imageId));
    }

    BlobStorage.StoredBlob imageContent(ProductImageEntity image) {
        return blobStorage.retrieve(image.blobReference());
    }

    private UUID requireVendorId(UUID callerAccountId) {
        return vendors
                .findByOwnerAccountId(callerAccountId)
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN, "Only a vendor may manage products."))
                .id();
    }

    /** Same repo-level enforcement as {@link #getMine} — see {@code findByIdAndVendorId}'s javadoc. */
    private ProductEntity requireOwned(UUID productId, UUID callerAccountId) {
        UUID vendorId = requireVendorId(callerAccountId);
        return products.findByIdAndVendorId(productId, vendorId).orElseGet(() -> {
            logCrossVendorAttempt(productId, callerAccountId, vendorId);
            throw ApiException.notFound("Product", productId);
        });
    }

    private void logCrossVendorAttempt(UUID productId, UUID callerAccountId, UUID callerVendorId) {
        log.warn(
                "Cross-vendor access attempt: account {} (vendor {}) requested product {} owned by a different vendor",
                callerAccountId,
                callerVendorId,
                productId);
    }
}
