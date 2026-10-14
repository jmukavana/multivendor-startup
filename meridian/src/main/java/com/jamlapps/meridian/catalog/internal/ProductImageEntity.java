package com.jamlapps.meridian.catalog.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** BR-CAT-05. {@code blobReference} resolves through {@code shared.BlobStorage}, never a path. */
@Entity
@Table(name = "product_image")
class ProductImageEntity {

    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "blob_reference", nullable = false)
    private String blobReference;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    protected ProductImageEntity() {
        // for JPA
    }

    ProductImageEntity(UUID id, UUID productId, String blobReference, String contentType, int sortOrder, Instant uploadedAt) {
        this.id = id;
        this.productId = productId;
        this.blobReference = blobReference;
        this.contentType = contentType;
        this.sortOrder = sortOrder;
        this.uploadedAt = uploadedAt;
    }

    UUID id() {
        return id;
    }

    UUID productId() {
        return productId;
    }

    String blobReference() {
        return blobReference;
    }

    String contentType() {
        return contentType;
    }

    int sortOrder() {
        return sortOrder;
    }
}
