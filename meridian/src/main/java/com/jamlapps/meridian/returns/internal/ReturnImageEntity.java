package com.jamlapps.meridian.returns.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** BR-RTN-02. {@code blobReference} resolves through {@code shared.BlobStorage}, never a path — matching {@code catalog.ProductImageEntity} exactly. */
@Entity
@Table(name = "return_image")
class ReturnImageEntity {

    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "return_id", nullable = false)
    private ReturnEntity returnRequest;

    @Column(name = "blob_reference", nullable = false)
    private String blobReference;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    protected ReturnImageEntity() {
        // for JPA
    }

    ReturnImageEntity(UUID id, ReturnEntity returnRequest, String blobReference, String contentType, Instant uploadedAt) {
        this.id = id;
        this.returnRequest = returnRequest;
        this.blobReference = blobReference;
        this.contentType = contentType;
        this.uploadedAt = uploadedAt;
    }

    String blobReference() {
        return blobReference;
    }
}
