package com.jamlapps.meridian.integrity.internal;

import com.jamlapps.meridian.fulfilment.VendorDispatchActivity;
import com.jamlapps.meridian.integrity.BuyerAbuseSignal;
import com.jamlapps.meridian.integrity.VendorFraudSignal;
import com.jamlapps.meridian.order.BuyerOrderActivity;
import com.jamlapps.meridian.returns.BuyerReturnActivity;
import com.jamlapps.meridian.vendor.Vendors;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-SEC-04/05: "materially above the platform median" — computed here, not sourced from
 * configuration, as {@link #MATERIALITY_MULTIPLIER}. A buyer or vendor's own rate is compared
 * against {@code MATERIALITY_MULTIPLIER} times the platform median for that same metric; when the
 * median itself is zero (every buyer at zero cancellations is a real, common case at low volume —
 * this codebase's own tests included), that comparison is meaningless (anything above zero times a
 * multiplier is still zero), so {@link #FLOOR_RATE} is the fallback: a rate above it is flagged
 * outright regardless of what the median says. Neither constant is business-tunable configuration
 * yet — the same "not yet sourced from configuration" treatment every other undedicated business
 * constant in this codebase gets (see {@code inventory.internal.InventoryService.RESERVATION_TTL}).
 *
 * <p>Every computation here reads real per-buyer or per-vendor counts from the module that owns
 * them — {@code order.BuyerOrderActivity}, {@code returns.BuyerReturnActivity}, {@code
 * fulfilment.VendorDispatchActivity}, {@code vendor.Vendors} — and only aggregates; none of those
 * counts are recomputed or duplicated here.
 */
@Service
class AbuseDetectionService {

    static final BigDecimal MATERIALITY_MULTIPLIER = BigDecimal.valueOf(2);
    static final BigDecimal FLOOR_RATE = new BigDecimal("0.5");

    private final BuyerOrderActivity orderActivity;
    private final BuyerReturnActivity returnActivity;
    private final VendorDispatchActivity dispatchActivity;
    private final Vendors vendors;

    AbuseDetectionService(
            BuyerOrderActivity orderActivity, BuyerReturnActivity returnActivity, VendorDispatchActivity dispatchActivity, Vendors vendors) {
        this.orderActivity = orderActivity;
        this.returnActivity = returnActivity;
        this.dispatchActivity = dispatchActivity;
        this.vendors = vendors;
    }

    @Transactional(readOnly = true)
    List<BuyerAbuseSignal> buyerAbuseSignals() {
        Map<UUID, BuyerOrderActivity.BuyerActivityStat> orderStats = new HashMap<>();
        orderActivity.cancellationStats().forEach(stat -> orderStats.put(stat.buyerAccountId(), stat));

        Map<UUID, BuyerReturnActivity.BuyerReturnStat> returnStats = new HashMap<>();
        returnActivity.returnStats().forEach(stat -> returnStats.put(stat.buyerAccountId(), stat));

        record Rates(UUID buyerAccountId, BigDecimal cancellationRate, BigDecimal returnRate, BigDecimal disputeRate) {}

        List<Rates> rates = orderStats.values().stream()
                .map(order -> {
                    BuyerReturnActivity.BuyerReturnStat returned =
                            returnStats.getOrDefault(order.buyerAccountId(), new BuyerReturnActivity.BuyerReturnStat(order.buyerAccountId(), 0, 0));
                    return new Rates(
                            order.buyerAccountId(),
                            rate(order.cancelledSubOrders(), order.totalSubOrders()),
                            rate(returned.totalReturns(), order.totalSubOrders()),
                            rate(returned.disputedReturns(), returned.totalReturns()));
                })
                .toList();

        BigDecimal cancellationMedian = median(rates.stream().map(Rates::cancellationRate).toList());
        BigDecimal returnMedian = median(rates.stream().map(Rates::returnRate).toList());
        BigDecimal disputeMedian = median(rates.stream().map(Rates::disputeRate).toList());

        return rates.stream()
                .map(r -> new BuyerAbuseSignal(
                        r.buyerAccountId(),
                        r.cancellationRate(),
                        r.returnRate(),
                        r.disputeRate(),
                        isMateriallyAbove(r.cancellationRate(), cancellationMedian)
                                || isMateriallyAbove(r.returnRate(), returnMedian)
                                || isMateriallyAbove(r.disputeRate(), disputeMedian)))
                .toList();
    }

    @Transactional(readOnly = true)
    List<VendorFraudSignal> vendorFraudSignals() {
        Set<UUID> inCoolingOff = Set.copyOf(vendors.vendorsInPayoutCoolingOff());

        List<VendorDispatchActivity.VendorDispatchStat> dispatch = dispatchActivity.dispatchStats();
        BigDecimal dispatchMedian = median(dispatch.stream().map(s -> rate(s.failedDeliveries(), s.totalDeliveries())).toList());

        return dispatch.stream()
                .map(s -> {
                    BigDecimal failureRate = rate(s.failedDeliveries(), s.totalDeliveries());
                    boolean payoutRecentlyChanged = inCoolingOff.contains(s.vendorId());
                    return new VendorFraudSignal(
                            s.vendorId(), failureRate, payoutRecentlyChanged, payoutRecentlyChanged || isMateriallyAbove(failureRate, dispatchMedian));
                })
                .toList();
    }

    private boolean isMateriallyAbove(BigDecimal rate, BigDecimal median) {
        return median.compareTo(BigDecimal.ZERO) > 0
                ? rate.compareTo(median.multiply(MATERIALITY_MULTIPLIER)) > 0
                : rate.compareTo(FLOOR_RATE) > 0;
    }

    private static BigDecimal rate(long numerator, long denominator) {
        return denominator == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 6, RoundingMode.HALF_UP);
    }

    private static BigDecimal median(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<BigDecimal> sorted = values.stream().sorted(Comparator.naturalOrder()).toList();
        int size = sorted.size();
        if (size % 2 == 1) {
            return sorted.get(size / 2);
        }
        return sorted.get(size / 2 - 1).add(sorted.get(size / 2)).divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
    }
}
