package com.jamlapps.meridian.admin.internal;

import com.jamlapps.meridian.order.Orders;
import com.jamlapps.meridian.order.ParentOrder;
import com.jamlapps.meridian.payment.Payments;
import com.jamlapps.meridian.web.ApiException;
import com.jamlapps.meridian.web.ErrorCode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * BR-ADM-02: order search across all vendors, by reference, buyer, or payment reference. "By
 * phone" is not built — see {@code admin.package-info} for why that dimension is genuinely
 * deferred rather than skipped for convenience.
 *
 * <p>BR-SEC-03's {@code sourceAddress} is a fifth criterion, added when that requirement was
 * built — real fraud investigation ("who else ordered from this same address") is exactly the
 * cross-vendor search BR-ADM-02 already exists to provide, not a separate feature.
 *
 * <p>Each supplied criterion contributes its own matches; the result is their union, deduplicated
 * by order id, not their intersection — an admin searching by both a reference and a buyer id at
 * once almost always means "either of these", the same way a support agent would read the request,
 * not "an order matching both simultaneously" (which a single {@code reference} search already
 * pins to at most one order anyway).
 */
@Service
class AdminOrderSearchService {

    private final Orders orders;
    private final Payments payments;

    AdminOrderSearchService(Orders orders, Payments payments) {
        this.orders = orders;
        this.payments = payments;
    }

    List<ParentOrder> search(
            @Nullable String reference, @Nullable UUID buyerAccountId, @Nullable String paymentReference, @Nullable String sourceAddress) {
        if (reference == null && buyerAccountId == null && paymentReference == null && sourceAddress == null) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR, "At least one of reference, buyerAccountId, paymentReference or sourceAddress is required.");
        }

        // LinkedHashMap: stable order for a caller paging through results by eye, and de-dupes by
        // order id when more than one criterion matches the same order.
        Map<UUID, ParentOrder> matches = new LinkedHashMap<>();

        if (reference != null) {
            orders.findByReference(reference).ifPresent(order -> matches.put(order.id(), order));
        }

        if (buyerAccountId != null) {
            for (ParentOrder order : orders.findByBuyerAccountId(buyerAccountId)) {
                matches.put(order.id(), order);
            }
        }

        if (paymentReference != null) {
            payments.findByProviderTransactionReference(paymentReference)
                    .flatMap(payment -> orders.findById(payment.parentOrderId()))
                    .ifPresent(order -> matches.put(order.id(), order));
        }

        if (sourceAddress != null) {
            for (ParentOrder order : orders.findBySourceAddress(sourceAddress)) {
                matches.put(order.id(), order);
            }
        }

        return new ArrayList<>(matches.values());
    }
}
