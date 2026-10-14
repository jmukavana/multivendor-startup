package com.jamlapps.meridian.order.internal;

import com.jamlapps.meridian.catalog.Product;
import com.jamlapps.meridian.catalog.Products;
import com.jamlapps.meridian.order.OrderLine;
import com.jamlapps.meridian.order.OrderLineRequest;
import com.jamlapps.meridian.order.OrderStatus;
import com.jamlapps.meridian.order.ParentOrder;
import com.jamlapps.meridian.order.RequestOrigin;
import com.jamlapps.meridian.order.SubOrder;
import com.jamlapps.meridian.order.events.OrderPaymentConfirmed;
import com.jamlapps.meridian.order.events.SubOrderCancelled;
import com.jamlapps.meridian.payment.RefundInitiation;
import com.jamlapps.meridian.pricing.PricingConfiguration;
import com.jamlapps.meridian.shared.Money;
import com.jamlapps.meridian.vendor.Vendors;
import com.jamlapps.meridian.web.ApiException;
import com.jamlapps.meridian.web.ErrorCode;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-ORD-01, BR-ORD-02, BR-ORD-10: order creation and the manual payment-confirmation step that
 * stands in for Phase 3's real payment integration.
 */
@Service
class OrderService {

    /** Collisions on an 8-character reference are effectively impossible; this bounds retry anyway. */
    private static final int MAX_REFERENCE_ATTEMPTS = 5;

    /**
     * BR-ORD-05's "configurable window" — not yet sourced from configuration, a constant here
     * rather than a business decision made silently, same treatment as {@code inventory.internal
     * .InventoryService.RESERVATION_TTL}.
     */
    static final Duration ACCEPTANCE_WINDOW = Duration.ofHours(48);

    private final ParentOrderRepository parentOrders;
    private final SubOrderRepository subOrders;
    private final OrderEventRepository orderEvents;
    private final Products products;
    private final PricingConfiguration pricing;
    private final Vendors vendors;
    private final RefundInitiation refundInitiation;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    OrderService(
            ParentOrderRepository parentOrders,
            SubOrderRepository subOrders,
            OrderEventRepository orderEvents,
            Products products,
            PricingConfiguration pricing,
            Vendors vendors,
            RefundInitiation refundInitiation,
            ApplicationEventPublisher events,
            Clock clock) {
        this.parentOrders = parentOrders;
        this.subOrders = subOrders;
        this.orderEvents = orderEvents;
        this.products = products;
        this.pricing = pricing;
        this.vendors = vendors;
        this.refundInitiation = refundInitiation;
        this.events = events;
        this.clock = clock;
    }

    /**
     * BR-ORD-01/02: creates a parent order and splits {@code lines} into one sub-order per vendor.
     * Every product is re-fetched from {@code catalog} here, which is BR-CHK-04's revalidation —
     * whatever price or availability the caller's cart assumed, this uses catalog's current state,
     * not a value the caller supplied.
     *
     * <p>Deliberately does not touch inventory. {@code checkout} — the only caller, via {@link
     * com.jamlapps.meridian.order.OrderCreation} — reserves stock in the same transaction as this
     * call, and rolls both back together if reservation fails; see that interface's javadoc for
     * why this is a synchronous call rather than an event.
     */
    @Transactional
    ParentOrder create(
            UUID buyerAccountId, List<OrderLineRequest> lines, Map<UUID, Money> deliveryCostByVendor, RequestOrigin origin) {
        if (lines.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "An order needs at least one line.");
        }

        // Grouped in encounter order, not sorted, so a single-vendor cart with several lines still
        // produces one sub-order rather than however many the caller happened to list.
        Map<UUID, List<OrderLineRequest>> linesByVendor = new LinkedHashMap<>();
        Map<UUID, Product> resolvedProducts = new LinkedHashMap<>();

        for (OrderLineRequest line : lines) {
            if (line.quantity() <= 0) {
                throw ApiException.validation(
                        "quantity must be positive", Map.of("quantity", List.of("must be positive")));
            }

            Product product = products
                    .findById(line.productId())
                    .orElseThrow(() -> ApiException.notFound("Product", line.productId()));

            if (!product.isOrderable()) {
                throw new ApiException(
                        ErrorCode.VALIDATION_ERROR, "Product \"" + product.name() + "\" is not available for order.");
            }

            resolvedProducts.put(line.productId(), product);
            linesByVendor.computeIfAbsent(product.vendorId(), key -> new java.util.ArrayList<>()).add(line);
        }

        // Every sub-order needs a currency for its own subtotal before any line is added; taken
        // from that vendor group's first product, matching how the parent's currency is taken from
        // the first vendor group overall. Mixed-currency vendors are out of scope (DAT-01 assumes
        // one currency per amount, and nothing in this codebase yet models a vendor pricing in more
        // than one).
        Instant now = clock.instant();
        Currency currency = resolvedProducts.values().iterator().next().price().currency();
        ParentOrderEntity parent = createParentWithRetry(buyerAccountId, currency, origin, now);

        // COM-01/02/05, BR-ADM-06: resolved once per order, not once per vendor group — every
        // sub-order in one checkout is priced under the same commission and holdback configuration
        // versions, which is what "the rate in force at order creation" means for an order as a
        // whole.
        UUID commissionConfigurationId = pricing.currentCommissionRate().configurationVersionId();
        UUID holdbackConfigurationId = pricing.currentHoldbackRate().configurationVersionId();

        for (var entry : linesByVendor.entrySet()) {
            UUID vendorId = entry.getKey();
            Money deliveryCost = deliveryCostByVendor.getOrDefault(vendorId, Money.zero(currency));
            SubOrderEntity subOrder =
                    parent.addSubOrder(vendorId, currency, commissionConfigurationId, holdbackConfigurationId, deliveryCost, now);

            for (OrderLineRequest line : entry.getValue()) {
                Product product = resolvedProducts.get(line.productId());
                subOrder.addLine(product.id(), product.name(), product.price(), line.quantity());
            }
        }

        parent.recalculateTotal();
        parentOrders.save(parent);
        recordEvent(parent.id(), "ORDER_CREATED", null, parent.status().name(), buyerAccountId, now);

        return parent.toParentOrder();
    }

    @Transactional(readOnly = true)
    ParentOrder get(UUID parentOrderId, UUID callerAccountId) {
        // NOT_FOUND rather than FORBIDDEN: confirming an order id exists but belongs to someone
        // else is itself a disclosure. Enforced in the WHERE clause via findByIdAndBuyerAccountId,
        // not by fetching the row and checking it here — see that method's javadoc.
        return parentOrders
                .findByIdAndBuyerAccountId(parentOrderId, callerAccountId)
                .map(ParentOrderEntity::toParentOrder)
                .orElseThrow(() -> ApiException.notFound("Order", parentOrderId));
    }

    @Transactional(readOnly = true)
    Page<ParentOrder> listForBuyer(UUID buyerAccountId, Pageable pageable) {
        return parentOrders
                .findByBuyerAccountIdOrderByCreatedAtDesc(buyerAccountId, pageable)
                .map(ParentOrderEntity::toParentOrder);
    }

    /** The vendor-dashboard equivalent of {@link #listForBuyer} — one vendor's own order queue. */
    @Transactional(readOnly = true)
    Page<SubOrder> listForVendor(UUID callerAccountId, Pageable pageable) {
        UUID vendorId = vendors
                .findByOwnerAccountId(callerAccountId)
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN, "Only a vendor may view its own orders."))
                .id();

        return subOrders.findByVendorIdOrderByCreatedAtDesc(vendorId, pageable).map(SubOrderEntity::toSubOrder);
    }

    /**
     * BR-PAY-04: "confirmed only upon a verified provider callback, never upon prompt initiation".
     * The only caller is {@code PaymentConfirmationListener}, reacting to
     * {@code payment.events.PaymentConfirmed} — there is deliberately no HTTP path to this method
     * any more; Phase 1's manual {@code POST /orders/{id}/mark-payment-confirmed} was a stand-in
     * for exactly this and is retired now that Phase 3 provides the real trigger, the same way
     * checkout retired direct order creation in Phase 4.
     *
     * @param actorAccountId null for a provider-driven confirmation — there is no human actor
     */
    @Transactional
    ParentOrder markPaymentConfirmed(UUID parentOrderId, @Nullable UUID actorAccountId) {
        ParentOrderEntity order = parentOrders
                .findById(parentOrderId)
                .orElseThrow(() -> ApiException.notFound("Order", parentOrderId));

        if (order.status() != OrderStatus.PENDING_PAYMENT) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "Order " + order.reference() + " is not awaiting payment.");
        }

        String previousStatus = order.status().name();
        Instant now = clock.instant();
        order.markConfirmed(now, now.plus(ACCEPTANCE_WINDOW));
        parentOrders.save(order);

        recordEvent(order.id(), "PAYMENT_CONFIRMED", previousStatus, order.status().name(), actorAccountId, now);

        // COM-05, BR-ADM-06: both rates resolved here are the ones stored at order creation, via
        // the same commissionConfigurationId/holdbackConfigurationId — not "the current rate" —
        // see PricingConfiguration's javadoc.
        List<OrderPaymentConfirmed.SubOrderSettlementDetail> settlementDetails = order.subOrders().stream()
                .map(subOrder -> new OrderPaymentConfirmed.SubOrderSettlementDetail(
                        subOrder.id(),
                        subOrder.vendorId(),
                        subOrder.subtotal(),
                        pricing.rateForVersion(subOrder.commissionConfigurationId()).rate(),
                        subOrder.commissionConfigurationId(),
                        pricing.holdbackRateForVersion(subOrder.holdbackConfigurationId()).rate(),
                        subOrder.holdbackConfigurationId()))
                .toList();

        events.publishEvent(new OrderPaymentConfirmed(
                order.id(), order.reference(), order.buyerAccountId(), settlementDetails, now));

        return order.toParentOrder();
    }

    /**
     * BR-RTN-01's return-window marker. Stays vendor-invoked and ownership-checked, exactly as it
     * was before {@code fulfilment} existed — see {@code OrderStatus}'s javadoc for why {@code
     * fulfilment}'s own, much richer delivery tracking (BOOKED/DISPATCHED/IN_TRANSIT/DELIVERED)
     * could not become the trigger for this without creating a module dependency cycle, and is
     * therefore a genuinely separate, independently-triggered "delivered" from this one.
     */
    @Transactional
    void markDelivered(UUID subOrderId, UUID callerAccountId) {
        UUID vendorId = vendors
                .findByOwnerAccountId(callerAccountId)
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN, "Only a vendor may mark a sub-order delivered."))
                .id();

        SubOrderEntity subOrder = subOrders
                .findByIdAndVendorId(subOrderId, vendorId)
                .orElseThrow(() -> ApiException.notFound("SubOrder", subOrderId));

        if (subOrder.status() != OrderStatus.CONFIRMED) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Sub-order " + subOrderId + " is not awaiting delivery.");
        }

        Instant now = clock.instant();
        String previousStatus = subOrder.status().name();
        subOrder.markDelivered(now);
        subOrders.save(subOrder);

        recordEvent(subOrder.parentOrderId(), "SUB_ORDER_DELIVERED", previousStatus, subOrder.status().name(), callerAccountId, now);
    }

    /**
     * BR-ORD-05: vendor acceptance. {@link #accept} and {@link #autoCancelUnaccepted} are this
     * requirement's two outcomes for one CONFIRMED sub-order — a vendor reaching this before the
     * window closes, or {@link AcceptanceWindowSweeper} reaching it after.
     */
    @Transactional
    SubOrder accept(UUID subOrderId, UUID callerAccountId) {
        UUID vendorId = vendors
                .findByOwnerAccountId(callerAccountId)
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN, "Only a vendor may accept a sub-order."))
                .id();

        SubOrderEntity subOrder = subOrders
                .findByIdAndVendorId(subOrderId, vendorId)
                .orElseThrow(() -> ApiException.notFound("SubOrder", subOrderId));

        if (subOrder.status() != OrderStatus.CONFIRMED) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Sub-order " + subOrderId + " is not awaiting acceptance.");
        }

        Instant now = clock.instant();
        String previousStatus = subOrder.status().name();
        subOrder.accept(now);
        subOrders.save(subOrder);

        recordEvent(subOrder.parentOrderId(), "SUB_ORDER_ACCEPTED", previousStatus, subOrder.status().name(), callerAccountId, now);

        return subOrder.toSubOrder();
    }

    /**
     * BR-ORD-05's other outcome. Called per id from {@link AcceptanceWindowSweeper}, through the
     * injected, proxied bean rather than looped from inside this class — matching every other
     * sweeper's own reasoning in this codebase. Re-checks {@code isAwaitingAcceptance} rather than
     * trusting the sweeper's own candidate list is still accurate by the time this runs: a vendor
     * accepting between the sweep's read and this call is a real, harmless race this guard exists
     * for, not a defensive no-op.
     */
    @Transactional
    void autoCancelUnaccepted(UUID subOrderId) {
        SubOrderEntity subOrder = subOrders.findById(subOrderId).orElseThrow(() -> ApiException.notFound("SubOrder", subOrderId));

        if (!subOrder.isAwaitingAcceptance()) {
            return;
        }

        cancelInternal(subOrder, null, "Vendor did not accept within the acceptance window");
    }

    @Transactional(readOnly = true)
    List<UUID> unacceptedPastDeadlineIds() {
        return subOrders.findByStatusAndAcceptanceDeadlineBefore(OrderStatus.CONFIRMED, clock.instant()).stream()
                .map(SubOrderEntity::id)
                .toList();
    }

    /**
     * BR-ORD-13/14: cancels one sub-order — never the parent order or its other sub-orders
     * (BR-ORD-06). If payment had already been confirmed, the refund is issued synchronously
     * before the transition commits — {@code RefundInitiation}'s own reasoning, see {@code
     * package-info.java} — so a cancellation can never be recorded without the money actually
     * having been returned. {@code SubOrderCancelled} then carries everything {@code inventory}
     * (release-or-restock) and {@code ledger} (commission reversal, only if a refund happened)
     * need, without either module gaining a further dependency.
     */
    @Transactional
    SubOrder cancel(UUID subOrderId, UUID buyerAccountId) {
        SubOrderEntity subOrder = subOrders.findById(subOrderId).orElseThrow(() -> ApiException.notFound("SubOrder", subOrderId));

        // NOT_FOUND, not FORBIDDEN — the same cross-buyer disclosure reasoning as OrderService.get.
        if (!subOrder.buyerAccountId().equals(buyerAccountId)) {
            throw ApiException.notFound("SubOrder", subOrderId);
        }

        return cancelInternal(subOrder, buyerAccountId, "Buyer cancellation");
    }

    /**
     * Shared by {@link #cancel} (buyer-invoked, BR-ORD-13) and {@link #autoCancelUnaccepted}
     * (system-invoked, BR-ORD-05) — the mechanics are identical either way; only who is acting,
     * and why, differ. {@code actorAccountId} is null for the system path, the same convention
     * {@link #markPaymentConfirmed} already uses for its own provider-driven transition.
     */
    private SubOrder cancelInternal(SubOrderEntity subOrder, @Nullable UUID actorAccountId, String reason) {
        if (!subOrder.isCancellable()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "Sub-order " + subOrder.id() + " can no longer be cancelled; use a return instead.");
        }

        // ACCEPTED implies CONFIRMED already happened — payment was confirmed either way.
        boolean wasConfirmed = subOrder.status() == OrderStatus.CONFIRMED || subOrder.status() == OrderStatus.ACCEPTED;
        BigDecimal commissionRate = pricing.rateForVersion(subOrder.commissionConfigurationId()).rate();
        BigDecimal holdbackRate = pricing.holdbackRateForVersion(subOrder.holdbackConfigurationId()).rate();

        if (wasConfirmed) {
            // BR-CHK-06/07: the buyer paid subtotal() plus deliveryCost() together as one charge —
            // cancelling refunds both, {@code subOrder.total()}, not just the items. This is
            // deliberately a different figure from SubOrderCancelled.refundAmount below, which
            // stays items-only on purpose — see that field's own javadoc for why.
            refundInitiation.initiate(subOrder.parentOrderId(), subOrder.total(), reason);
        }

        Instant now = clock.instant();
        String previousStatus = subOrder.status().name();
        subOrder.cancel(now);
        subOrders.save(subOrder);

        recordEvent(subOrder.parentOrderId(), "SUB_ORDER_CANCELLED", previousStatus, subOrder.status().name(), actorAccountId, now);

        events.publishEvent(new SubOrderCancelled(
                subOrder.id(),
                subOrder.parentOrderId(),
                subOrder.vendorId(),
                subOrder.buyerAccountId(),
                wasConfirmed,
                subOrder.subtotal(),
                commissionRate,
                subOrder.commissionConfigurationId(),
                holdbackRate,
                subOrder.holdbackConfigurationId(),
                subOrder.lines().stream()
                        .map(line -> {
                            OrderLine orderLine = line.toOrderLine();
                            return new SubOrderCancelled.CancelledLine(orderLine.productId(), orderLine.quantity());
                        })
                        .toList(),
                reason,
                now));

        return subOrder.toSubOrder();
    }

    private ParentOrderEntity createParentWithRetry(UUID buyerAccountId, Currency currency, RequestOrigin origin, Instant now) {
        for (int attempt = 1; attempt <= MAX_REFERENCE_ATTEMPTS; attempt++) {
            try {
                ParentOrderEntity parent = new ParentOrderEntity(
                        UUID.randomUUID(), OrderReferenceGenerator.generate(), buyerAccountId, currency, origin, now);
                parentOrders.saveAndFlush(parent);
                return parent;
            } catch (DataIntegrityViolationException collision) {
                if (attempt == MAX_REFERENCE_ATTEMPTS) {
                    throw collision;
                }
            }
        }
        throw new IllegalStateException("unreachable");
    }

    private void recordEvent(
            UUID parentOrderId,
            String eventType,
            @Nullable String previousStatus,
            @Nullable String newStatus,
            @Nullable UUID actorAccountId,
            Instant now) {
        orderEvents.save(new OrderEventEntity(
                UUID.randomUUID(), parentOrderId, eventType, previousStatus, newStatus, actorAccountId, now));
    }
}
