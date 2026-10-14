package com.jamlapps.meridian.vendor.internal;

import com.jamlapps.meridian.shared.BlobStorage;
import com.jamlapps.meridian.web.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** BR-VND-03. */
@Service
class VerificationDocumentService {

    private final VerificationDocumentRepository documents;
    private final VendorApplicationRepository applications;
    private final BlobStorage blobStorage;
    private final Clock clock;

    VerificationDocumentService(
            VerificationDocumentRepository documents,
            VendorApplicationRepository applications,
            BlobStorage blobStorage,
            Clock clock) {
        this.documents = documents;
        this.applications = applications;
        this.blobStorage = blobStorage;
        this.clock = clock;
    }

    @Transactional
    void upload(
            UUID applicationId,
            UUID callerAccountId,
            VerificationDocumentEntity.DocumentType type,
            InputStream content,
            String originalFilename,
            String contentType,
            long sizeBytes) {

        var application = applications
                .findById(applicationId)
                .orElseThrow(() -> ApiException.notFound("Vendor application", applicationId));

        if (!application.accountId().equals(callerAccountId)) {
            throw ApiException.notFound("Vendor application", applicationId);
        }

        String reference;
        try (content) {
            reference = blobStorage.store(content, contentType, sizeBytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        documents.save(new VerificationDocumentEntity(
                UUID.randomUUID(),
                applicationId,
                type,
                reference,
                originalFilename,
                contentType,
                sizeBytes,
                clock.instant()));
    }

    @Transactional(readOnly = true)
    List<VerificationDocumentEntity.DocumentType> listTypesUploaded(UUID applicationId) {
        return documents.findByApplicationId(applicationId).stream()
                .map(VerificationDocumentEntity::documentType)
                .toList();
    }
}
