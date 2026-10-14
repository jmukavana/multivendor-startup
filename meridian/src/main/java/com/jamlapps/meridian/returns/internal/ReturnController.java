package com.jamlapps.meridian.returns.internal;

import com.jamlapps.meridian.returns.Return;
import com.jamlapps.meridian.returns.ReturnReason;
import com.jamlapps.meridian.web.ApiException;
import com.jamlapps.meridian.web.ApiResponse;
import com.jamlapps.meridian.web.AuthenticatedCaller;
import com.jamlapps.meridian.web.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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

/** BR-RTN-01/02/03/04, buyer- and vendor-facing. */
@RestController
@RequestMapping("/returns")
class ReturnController {

    private static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024;

    private final ReturnService service;

    ReturnController(ReturnService service) {
        this.service = service;
    }

    record RequestReturnRequest(
            @NotNull(message = "must be provided") UUID subOrderId,
            @NotNull(message = "must be provided") ReturnReason reason,
            @Nullable String description,
            @NotEmpty(message = "a return needs at least one line") List<ReturnService.ReturnLineRequest> lines) {}

    record VendorResponseRequest(boolean accept) {}

    @PostMapping
    ApiResponse<Return> request(@Valid @RequestBody RequestReturnRequest request, @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.request(
                request.subOrderId(), principal.accountId(), request.reason(), request.description(), request.lines()));
    }

    /** BR-RTN-02. */
    @PostMapping("/{returnId}/images")
    ApiResponse<Void> addImage(
            @PathVariable UUID returnId, @RequestParam @NotNull MultipartFile file, @AuthenticationPrincipal AuthenticatedCaller principal) {

        if (file.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "The uploaded file is empty.");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "The uploaded image exceeds the 5MB limit.");
        }

        try {
            service.addImage(
                    returnId,
                    principal.accountId(),
                    file.getInputStream(),
                    file.getContentType() == null ? "application/octet-stream" : file.getContentType(),
                    file.getSize());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return ApiResponse.of(null);
    }

    @GetMapping("/mine")
    ApiResponse<List<Return>> mine(@AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.listMine(principal.accountId()));
    }

    @GetMapping("/{returnId}")
    ApiResponse<Return> get(@PathVariable UUID returnId, @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.get(returnId, principal.accountId()));
    }

    /** BR-RTN-03. */
    @PostMapping("/{returnId}/vendor-response")
    ApiResponse<Return> vendorResponse(
            @PathVariable UUID returnId,
            @Valid @RequestBody VendorResponseRequest request,
            @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.vendorRespond(returnId, principal.accountId(), request.accept()));
    }
}
