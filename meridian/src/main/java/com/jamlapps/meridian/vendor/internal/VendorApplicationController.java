package com.jamlapps.meridian.vendor.internal;

import com.jamlapps.meridian.vendor.VendorApplication;
import com.jamlapps.meridian.web.ApiException;
import com.jamlapps.meridian.web.ApiResponse;
import com.jamlapps.meridian.web.AuthenticatedCaller;
import com.jamlapps.meridian.web.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * BR-VND-01/02/04: application submission. Field names and the response shape match
 * {@code meridian-frontend/src/features/vendor-onboarding} exactly — {@code businessName},
 * {@code registrationNumber?}, {@code kraPin}, {@code physicalAddress}, {@code county},
 * {@code contactName}, {@code contactPhone}, {@code contactEmail?} in, and {@code id}/{@code status}/
 * {@code businessName}/{@code submittedAt} are what that feature's {@code VendorApplication} type
 * reads back out (extra fields in the response are harmless to it — TypeScript's structural typing
 * ignores what it does not declare).
 *
 * <p><strong>The frontend sends an {@code idempotency-key} header on this request and this
 * endpoint does not honour it yet.</strong> A double submission from a network retry is only
 * partially guarded — see {@code VendorApplicationService.submit}'s KRA-PIN uniqueness check, which
 * rejects a genuine duplicate rather than silently creating two applications, but does not return
 * the original result gracefully. Generic idempotency-key handling belongs in {@code web} as a
 * cross-cutting concern, not built once here and once in {@code order}; it is a recorded gap.
 */
@RestController
@RequestMapping("/vendors/applications")
class VendorApplicationController {

    private static final long MAX_DOCUMENT_BYTES = 10 * 1024 * 1024;

    private final VendorApplicationService service;
    private final VerificationDocumentService documents;

    VendorApplicationController(VendorApplicationService service, VerificationDocumentService documents) {
        this.service = service;
        this.documents = documents;
    }

    record SubmitApplicationRequest(
            @NotBlank(message = "must not be blank") String businessName,
            @Nullable String registrationNumber,
            @NotBlank(message = "must not be blank") String kraPin,
            @NotBlank(message = "must not be blank") String physicalAddress,
            @NotBlank(message = "must not be blank") String county,
            @NotBlank(message = "must not be blank") String contactName,
            @NotBlank(message = "must not be blank") String contactPhone,
            @Email(message = "must be a valid email address") @Nullable String contactEmail) {}

    @PostMapping
    ApiResponse<VendorApplication> submit(
            @Valid @RequestBody SubmitApplicationRequest request,
            @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.submit(
                principal.accountId(),
                request.businessName(),
                request.registrationNumber(),
                request.kraPin(),
                request.physicalAddress(),
                request.county(),
                request.contactName(),
                request.contactPhone(),
                request.contactEmail()));
    }

    @GetMapping("/mine")
    ApiResponse<List<VendorApplication>> listMine(@AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.listMine(principal.accountId()));
    }

    @GetMapping("/{applicationId}")
    ApiResponse<VendorApplication> get(
            @PathVariable UUID applicationId, @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.get(applicationId, principal.accountId()));
    }

    /**
     * BR-VND-03. {@code documentType} matches
     * {@code VerificationDocumentEntity.DocumentType}: {@code NATIONAL_ID},
     * {@code BUSINESS_REGISTRATION_CERTIFICATE}, {@code KRA_PIN_CERTIFICATE}.
     *
     * <p>Ownership is enforced in the service, not here — the same NOT_FOUND-on-mismatch pattern
     * as everywhere else a caller might probe another party's record by id.
     */
    @PostMapping("/{applicationId}/documents")
    ApiResponse<Void> uploadDocument(
            @PathVariable UUID applicationId,
            @RequestParam VerificationDocumentEntity.DocumentType documentType,
            @RequestParam MultipartFile file,
            @AuthenticationPrincipal AuthenticatedCaller principal) {

        if (file.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "The uploaded file is empty.");
        }
        if (file.getSize() > MAX_DOCUMENT_BYTES) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "The uploaded file exceeds the 10MB limit.");
        }

        try {
            documents.upload(
                    applicationId,
                    principal.accountId(),
                    documentType,
                    file.getInputStream(),
                    file.getOriginalFilename() == null ? "document" : file.getOriginalFilename(),
                    file.getContentType() == null ? "application/octet-stream" : file.getContentType(),
                    file.getSize());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return ApiResponse.of(null);
    }
}
