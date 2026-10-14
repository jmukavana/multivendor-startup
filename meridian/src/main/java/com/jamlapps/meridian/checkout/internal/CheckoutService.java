package com.jamlapps.meridian.checkout.internal;

import com.jamlapps.meridian.catalog.Product;
import com.jamlapps.meridian.catalog.Products;
import com.jamlapps.meridian.fulfilment.DeliveryMethod;
import com.jamlapps.meridian.fulfilment.DeliveryQuote;
import com.jamlapps.meridian.fulfilment.DeliveryQuotes;
import com.jamlapps.meridian.fulfilment.DeliverySelections;
import com.jamlapps.meridian.inventory.InsufficientStockException;
import com.jamlapps.meridian.inventory.Reservations;
import com.jamlapps.meridian.order.OrderCreation;
import com.jamlapps.meridian.order.OrderLineRequest;
import com.jamlapps.meridian.order.ParentOrder;
import com.jamlapps.meridian.order.RequestOrigin;
import com.jamlapps.meridian.order.SubOrder;
import com.jamlapps.meridian.payment.PaymentInitiation;
import com.jamlapps.meridian.shared.Money;
import com.jamlapps.meridian.vendor.Vendors;
import com.jamlapps.meridian.web.ApiException;
import com.jamlapps.meridian.web.ErrorCode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-CHK-01/02/04/05/06/07/09: UC-03's real entry point. Orchestrates, and deliberately owns no
 * state of its own beyond this one request — BR-CHK-03 (persisting a cart across sessions) is
 * Should, not Must, in v2.2's MoSCoW re-baseline, so the caller submits the cart contents (and, as
 * of this pass, the delivery destination and method) on every call rather than this module storing
 * either.
 *
 * <p>One {@code @Transactional} method spanning cross-module calls into {@code order}, {@code
 * inventory} and {@code fulfilment} is the point of {@link OrderCreation}, {@link Reservations}
 * and {@link DeliverySelections} existing as direct calls rather than events: if any of them fails
 * partway through, the whole transaction — order and delivery selection rows included — rolls
 * back. A buyer never sees an order that could not get its stock, or one priced for delivery it
 * never actually got booked for.
 */
@Service
class CheckoutService {

    private final Products products;
    private final Vendors vendors;
    private final OrderCreation orderCreation;
    private final Reservations reservations;
    private final PaymentInitiation paymentInitiation;
    private final DeliveryQuotes deliveryQuotes;
    private final DeliverySelections deliverySelections;

    CheckoutService(
            Products products,
            Vendors vendors,
            OrderCreation orderCreation,
            Reservations reservations,
            PaymentInitiation paymentInitiation,
            DeliveryQuotes deliveryQuotes,
            DeliverySelections deliverySelections) {
        this.products = products;
        this.vendors = vendors;
        this.orderCreation = orderCreation;
        this.reservations = reservations;
        this.paymentInitiation = paymentInitiation;
        this.deliveryQuotes = deliveryQuotes;
        this.deliverySelections = deliverySelections;
    }

    /**
     * @param buyerPhoneNumber UC-03 step 7: collected at this same step, not stored earlier — a
     *     buyer may pay from a different handset than the one their account registered with
     * @param deliveryZoneId BR-CHK-05/08: one destination for the whole order — a real, if
     *     scaled-down, stand-in for BR-CHK-08's saved address book (not built; see this module's
     *     package doc), the same "submitted every call, not persisted" treatment BR-CHK-03's cart
     *     already gets
     * @param deliveryMethodByVendor BR-CHK-05: door or pickup point, per vendor group. A vendor
     *     group absent from this map defaults to DOOR — BR-CHK-05 says a buyer <em>may</em> choose,
     *     not that every checkout must name one explicitly for every vendor.
     * @param origin BR-SEC-03: the request's own device/session characteristics — resolved by
     *     {@code CheckoutController} from the same {@code HttpServletRequest} the rate limit
     *     above already reads, passed straight through to {@code order} without inspection here.
     */
    @Transactional
    ParentOrder checkout(
            UUID buyerAccountId,
            List<CartLine> lines,
            String buyerPhoneNumber,
            UUID deliveryZoneId,
            Map<UUID, DeliveryMethod> deliveryMethodByVendor,
            RequestOrigin origin) {
        if (lines.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "The cart is empty.");
        }

        // Resolved once, reused for the suspension check below and for BR-CHK-06's per-vendor
        // delivery quote — the same vendor grouping OrderService.create resolves independently on
        // its own side of the OrderCreation call, kept separate rather than shared because this
        // module and order have no other reason to agree on a shared "cart line" shape.
        Map<UUID, Product> resolvedProducts = new LinkedHashMap<>();
        for (CartLine line : lines) {
            resolvedProducts.put(line.productId(), products.findById(line.productId()).orElseThrow(() -> ApiException.notFound("Product", line.productId())));
        }

        // BR-CHK-09: checked here as well as being the natural effect of VendorSuspensionListener
        // delisting a suspended vendor's products (which order.create's own isOrderable() check
        // already refuses) — that listener is @Async (ADR-004), so there is a real window between
        // a suspension and its delisting taking effect. This closes it: a synchronous check
        // against current vendor status, not a stale read of product status alone.
        for (Product product : resolvedProducts.values()) {
            var vendor = vendors.findById(product.vendorId());
            if (vendor.isEmpty() || !vendor.get().isActive()) {
                throw new ApiException(
                        ErrorCode.VALIDATION_ERROR,
                        "\"" + product.name() + "\" is no longer available from its vendor.");
            }
        }

        // BR-CHK-06/07: one quote per vendor group, resolved and priced before the order exists —
        // never re-resolved later, the same versioned-value discipline commission rate already
        // follows (COM-02). A zone/method combination with no active carrier's rate card is a real
        // "delivery is not available" refusal, not something checkout silently prices at zero.
        Map<UUID, DeliveryQuote> quoteByVendor = new LinkedHashMap<>();
        for (Product product : resolvedProducts.values()) {
            UUID vendorId = product.vendorId();
            if (quoteByVendor.containsKey(vendorId)) {
                continue;
            }
            DeliveryMethod method = deliveryMethodByVendor.getOrDefault(vendorId, DeliveryMethod.DOOR);
            DeliveryQuote quote = deliveryQuotes
                    .cheapestQuote(deliveryZoneId, method)
                    .orElseThrow(() -> new ApiException(
                            ErrorCode.VALIDATION_ERROR,
                            "Delivery is not available to that zone by " + method + " for \"" + product.name() + "\"."));
            quoteByVendor.put(vendorId, quote);
        }

        Map<UUID, Money> deliveryMethodByVendorResolved = new LinkedHashMap<>();
        quoteByVendor.forEach((vendorId, quote) -> deliveryMethodByVendorResolved.put(vendorId, quote.price()));

        List<OrderLineRequest> orderLines =
                lines.stream().map(line -> new OrderLineRequest(line.productId(), line.quantity())).toList();

        // BR-ORD-01/02, BR-CHK-04: order re-fetches and revalidates every product itself — see
        // OrderCreation's javadoc — so this call is also where a stale client-side price or an
        // since-delisted product is caught, not assumed away by anything checkout passed in.
        ParentOrder order = orderCreation.create(buyerAccountId, orderLines, deliveryMethodByVendorResolved, origin);

        // BR-INV-02/04, AC-INV-01: reserved after the order exists so each reservation's
        // reference_id is the real parent order id from the first call — see
        // stock_reservation.reference_id's migration comment. Any failure here rolls back the
        // order created above along with it.
        for (CartLine line : lines) {
            try {
                reservations.reserve(line.productId(), line.quantity(), order.id());
            } catch (InsufficientStockException e) {
                throw new ApiException(
                        ErrorCode.VALIDATION_ERROR,
                        "Not enough stock for one of the items in your cart: only " + e.available()
                                + " available, " + e.requested() + " requested.");
            }
        }

        // BR-CHK-05/06: the buyer's delivery choice, recorded against each sub-order's now-real id
        // — see fulfilment.DeliverySelections' javadoc for why this, not OrderPaymentConfirmed's
        // own payload, is how fulfilment learns it once payment confirms.
        for (SubOrder subOrder : order.subOrders()) {
            DeliveryQuote quote = quoteByVendor.get(subOrder.vendorId());
            DeliveryMethod method = deliveryMethodByVendor.getOrDefault(subOrder.vendorId(), DeliveryMethod.DOOR);
            deliverySelections.select(subOrder.id(), quote.carrierId(), deliveryZoneId, method, quote.rateCardId());
        }

        // BR-PAY-02, UC-03 step 8: the payment prompt is initiated as part of the same checkout
        // call. The gateway call happens inside this transaction — acceptable while the only
        // implementation is MockPaymentGateway's effectively-instant no-op; a real provider's
        // network latency held open alongside the order and reservation locks above would need
        // revisiting this, most likely by moving initiation to a step after this transaction
        // commits rather than inside it.
        paymentInitiation.initiate(order.id(), order.total(), buyerAccountId, buyerPhoneNumber);

        return order;
    }

    record CartLine(UUID productId, int quantity) {}
}
