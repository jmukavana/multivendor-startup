package com.jamlapps.meridian.vendor.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** BR-VND-03. {@code blobReference} is resolved through {@code shared.BlobStorage}, never a path. */
@Entity
@Table(name = "verification_document")
class VerificationDocumentEntity {

    enum DocumentType {
        NATIONAL_ID,
        BUSINESS_REGISTRATION_CERTIFICATE,
        KRA_PIN_CERTIFICATE
    }

    @Id
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    @Column(name = "blob_reference", nullable = false)
    private String blobReference;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    protected VerificationDocumentEntity() {
        // for JPA
    }

    VerificationDocumentEntity(
            UUID id,
            UUID applicationId,
            DocumentType documentType,
            String blobReference,
            String originalFilename,
            String contentType,
            long sizeBytes,
            Instant uploadedAt) {
        this.id = id;
        this.applicationId = applicationId;
        this.documentType = documentType;
        this.blobReference = blobReference;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.uploadedAt = uploadedAt;
    }

    DocumentType documentType() {
        return documentType;
    }
}
