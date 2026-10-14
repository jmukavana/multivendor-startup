package com.jamlapps.meridian.catalog.internal;

import com.jamlapps.meridian.catalog.Product;
import com.jamlapps.meridian.shared.BlobStorage;
import com.jamlapps.meridian.shared.Money;
import com.jamlapps.meridian.web.ApiException;
import com.jamlapps.meridian.web.ApiResponse;
import com.jamlapps.meridian.web.AuthenticatedCaller;
import com.jamlapps.meridian.web.ErrorCode;
import com.jamlapps.meridian.web.SortSpec;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * BR-CAT-01/02/05/08/09, BR-SCH-01/04. Listing and detail are open to guests (BR-IAM-08) — see the
 * {@code permitAll} entry in {@code SecurityConfiguration} — because a buyer browsing before
 * registering is the normal case, not an edge case to defer.
 *
 * <p>The {@code /mine} endpoints trace to AC-CAT-01: a caller who is not the owning vendor gets a
 * 404 there (not a 403) for the same disclosure reasoning used elsewhere — see {@code
 * CatalogService.requireOwned}.
 */
@RestController
@RequestMapping("/catalog/products")
class CatalogController {

    private static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024;

    private final CatalogService service;

    CatalogController(CatalogService service) {
        this.service = service;
    }

    record CreateProductRequest(
            @NotBlank(message = "must not be blank") String name,
            @Nullable String description,
            @Min(value = 0, message = "must not be negative") long priceMinorUnits,
            @NotBlank(message = "must not be blank") String currency) {}

    record ChangePriceRequest(
            @Min(value = 0, message = "must not be negative") long priceMinorUnits,
            @NotBlank(message = "must not be blank") String currency) {}

    @PostMapping
    ApiResponse<Product> create(
            @Valid @RequestBody CreateProductRequest request, @AuthenticationPrincipal AuthenticatedCaller principal) {
        Money price = Money.of(request.priceMinorUnits(), Currency.getInstance(request.currency()));
        return ApiResponse.of(service.create(principal.accountId(), request.name(), request.description(), price));
    }

    @GetMapping("/{productId}")
    ApiResponse<Product> get(@PathVariable UUID productId) {
        return ApiResponse.of(service.get(productId));
    }

    @GetMapping
    ApiResponse<List<Product>> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int perPage) {

        var result = service.list(PageRequest.of(page, perPage));

        return ApiResponse.paged(
                result.getContent(),
                page,
                perPage,
                result.getTotalElements(),
                List.of(SortSpec.desc("createdAt")),
                Map.of());
    }

    @GetMapping("/mine")
    ApiResponse<List<Product>> listMine(@AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.listMine(principal.accountId()));
    }

    @GetMapping("/mine/{productId}")
    ApiResponse<Product> getMine(@PathVariable UUID productId, @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.getMine(productId, principal.accountId()));
    }

    /** BR-CAT-05/08, AC-CAT-02/03. */
    @PostMapping("/mine/{productId}/publish")
    ApiResponse<Product> publish(@PathVariable UUID productId, @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.publish(productId, principal.accountId()));
    }

    @PostMapping("/mine/{productId}/delist")
    ApiResponse<Product> delist(@PathVariable UUID productId, @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.delist(productId, principal.accountId()));
    }

    /** BR-CAT-09, AC-CAT-04. */
    @PutMapping("/mine/{productId}/price")
    ApiResponse<Product> changePrice(
            @PathVariable UUID productId,
            @Valid @RequestBody ChangePriceRequest request,
            @AuthenticationPrincipal AuthenticatedCaller principal) {
        Money newPrice = Money.of(request.priceMinorUnits(), Currency.getInstance(request.currency()));
        return ApiResponse.of(service.changePrice(productId, principal.accountId(), newPrice));
    }

    @GetMapping("/mine/{productId}/price-history")
    ApiResponse<List<ProductPriceChangeEntity.PriceChangeView>> priceHistory(
            @PathVariable UUID productId, @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.priceHistory(productId, principal.accountId()));
    }

    /** BR-CAT-05. */
    @PostMapping("/mine/{productId}/images")
    ApiResponse<Void> addImage(
            @PathVariable UUID productId,
            @RequestParam @NotNull MultipartFile file,
            @AuthenticationPrincipal AuthenticatedCaller principal) {

        if (file.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "The uploaded file is empty.");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "The uploaded image exceeds the 5MB limit.");
        }

        try {
            service.addImage(
                    productId,
                    principal.accountId(),
                    file.getInputStream(),
                    file.getContentType() == null ? "application/octet-stream" : file.getContentType(),
                    file.getSize());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return ApiResponse.of(null);
    }

    record ImageView(UUID id, String contentType, int sortOrder, String url) {}

    /** BR-CAT-05. Public like the product itself — see {@code SecurityConfiguration}'s permitAll for this path. */
    @GetMapping("/{productId}/images")
    ApiResponse<List<ImageView>> images(@PathVariable UUID productId) {
        List<ImageView> views = service.listImages(productId).stream()
                .map(image -> new ImageView(
                        image.id(),
                        image.contentType(),
                        image.sortOrder(),
                        "/catalog/products/" + productId + "/images/" + image.id() + "/content"))
                .toList();
        return ApiResponse.of(views);
    }

    /**
     * Raw bytes, not the {@code ApiResponse} envelope — this is what an {@code <img src>} fetches
     * directly. The reference is content-addressed and immutable once uploaded, so the response is
     * cacheable for a long time without a revalidation round trip.
     */
    @GetMapping("/{productId}/images/{imageId}/content")
    ResponseEntity<byte[]> imageContent(@PathVariable UUID productId, @PathVariable UUID imageId) {
        var image = service.image(productId, imageId);
        BlobStorage.StoredBlob blob = service.imageContent(image);

        byte[] bytes;
        try (var content = blob.content()) {
            bytes = content.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(blob.contentType()))
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                .body(bytes);
    }
}
