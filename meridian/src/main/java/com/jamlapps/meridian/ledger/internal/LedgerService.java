package com.jamlapps.meridian.ledger.internal;

import com.jamlapps.meridian.shared.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-LDG-01/02/03: the posting engine. Every posting is a list of {@link Line}s that must balance
 * — checked in Java before anything is saved (BR-LDG-05 depends on no unbalanced posting ever
 * reaching the table), and the whole method runs inside one transaction (BR-LDG-03).
 */
@Service
class LedgerService {

    private final LedgerEntryRepository entries;
    private final Clock clock;

    LedgerService(LedgerEntryRepository entries, Clock clock) {
        this.entries = entries;
        this.clock = clock;
    }

    record Line(LedgerAccount account, @Nullable UUID vendorId, boolean debit, Money amount) {}

    /**
     * BR-PAY-04→BR-LDG-02, ADR-007's rounding note: commission rounds {@code HALF_UP} — the
     * proposed convention in ADR-007, applied here because this is the one place a commission
     * amount is actually computed and posted. If ADR-007 is revised, this is the line to change.
     */
    @Transactional
    void postPaymentConfirmed(UUID subOrderId, UUID vendorId, Money subtotal, BigDecimal commissionRate, BigDecimal holdbackRate) {
        Money commission = subtotal.percentage(commissionRate, RoundingMode.HALF_UP);
        Money vendorPortion = subtotal.minus(commission);

        // BR-ADM-06: the rate {@code order} resolved and stored at order creation — see
        // pricing.PricingConfiguration's javadoc — carried here via OrderPaymentConfirmed, never
        // "the current holdback rate". Formerly a bare BigDecimal("0.10") constant; see
        // ledger/package-info.java for that history.
        Money holdback = vendorPortion.percentage(holdbackRate, RoundingMode.DOWN);
        Money availablePayable = vendorPortion.minus(holdback);

        List<Line> lines = new ArrayList<>();
        lines.add(new Line(LedgerAccount.COLLECTIONS_CLEARING, null, true, subtotal));
        lines.add(new Line(LedgerAccount.COMMISSION_REVENUE, null, false, commission));
        if (availablePayable.isPositive()) {
            lines.add(new Line(LedgerAccount.VENDOR_PAYABLE, vendorId, false, availablePayable));
        }
        if (holdback.isPositive()) {
            lines.add(new Line(LedgerAccount.HOLDBACK_RESERVE, vendorId, false, holdback));
        }

        post(lines, "SUB_ORDER", subOrderId);
    }

    /**
     * BR-RTN-05, BR-ORD-14: the exact contra of {@link #postPaymentConfirmed} for a refunded slice
     * of a sub-order's subtotal — same rate, same holdback split (the caller resolves both through
     * the sub-order's own stored configuration version ids, per BR-ADM-06, never "whatever is
     * current now"), signs flipped. DEBIT
     * COMMISSION_REVENUE (give back earned commission), DEBIT VENDOR_PAYABLE and/or
     * HOLDBACK_RESERVE (claw back the vendor's portion, from holdback first — see the split below),
     * CREDIT COLLECTIONS_CLEARING (the money actually leaving to reach the buyer). Debiting
     * HOLDBACK_RESERVE before VENDOR_PAYABLE is what makes BR-RTN-08 ("recover refunded amounts
     * from future vendor settlements where holdback is insufficient") hold: if a refund's holdback
     * portion exceeds what postPaymentConfirmed reserved for this vendor overall, HOLDBACK_RESERVE
     * simply goes negative — {@code VendorLedger.vendorsWithPayableBalance}'s {@code > 0} filter
     * already excludes a vendor whose balance a refund has driven negative from any future
     * settlement run, for free, with no separate "recovery" mechanism needed.
     *
     * <p>{@code referenceType} distinguishes a return-driven refund ({@code "RETURN"}, {@link
     * com.jamlapps.meridian.ledger.internal.RefundPostingListener}) from a cancellation-driven one
     * ({@code "SUB_ORDER_CANCELLATION"}, {@link CancellationRefundPostingListener}) in the ledger's
     * own audit trail — the posting math is identical either way, only its cause differs.
     */
    @Transactional
    void postRefund(
            UUID subOrderId, UUID vendorId, Money refundSubtotal, BigDecimal commissionRate, BigDecimal holdbackRate, String referenceType) {
        Money commission = refundSubtotal.percentage(commissionRate, RoundingMode.HALF_UP);
        Money vendorPortion = refundSubtotal.minus(commission);
        Money holdback = vendorPortion.percentage(holdbackRate, RoundingMode.DOWN);
        Money payable = vendorPortion.minus(holdback);

        List<Line> lines = new ArrayList<>();
        lines.add(new Line(LedgerAccount.COLLECTIONS_CLEARING, null, false, refundSubtotal));
        lines.add(new Line(LedgerAccount.COMMISSION_REVENUE, null, true, commission));
        if (payable.isPositive()) {
            lines.add(new Line(LedgerAccount.VENDOR_PAYABLE, vendorId, true, payable));
        }
        if (holdback.isPositive()) {
            lines.add(new Line(LedgerAccount.HOLDBACK_RESERVE, vendorId, true, holdback));
        }

        post(lines, referenceType, subOrderId);
    }

    @Transactional
    void postDisbursement(UUID vendorId, Money amount, UUID settlementRunId) {
        post(
                List.of(
                        new Line(LedgerAccount.VENDOR_PAYABLE, vendorId, true, amount),
                        new Line(LedgerAccount.COLLECTIONS_CLEARING, null, false, amount)),
                "SETTLEMENT_RUN",
                settlementRunId);
    }

    @Transactional
    void postHoldbackRelease(UUID vendorId, Money amount, UUID settlementRunId) {
        post(
                List.of(
                        new Line(LedgerAccount.HOLDBACK_RESERVE, vendorId, true, amount),
                        new Line(LedgerAccount.VENDOR_PAYABLE, vendorId, false, amount)),
                "SETTLEMENT_RUN",
                settlementRunId);
    }

    @Transactional(readOnly = true)
    Money balance(UUID vendorId, LedgerAccount account, Currency currency) {
        long minorUnits = entries.findByVendorIdAndAccount(vendorId, account).stream()
                .mapToLong(entry -> entry.isDebit() ? -entry.amountMinorUnits() : entry.amountMinorUnits())
                .sum();
        return Money.of(minorUnits, currency);
    }

    @Transactional(readOnly = true)
    List<UUID> vendorsWithPayableBalance(Currency currency) {
        return entries.vendorIdsWithPositivePayableBalance(currency.getCurrencyCode());
    }

    /** BR-LDG-05: sums every entry ever written, across every vendor and every account, to zero. */
    @Transactional(readOnly = true)
    boolean isReconciled() {
        long total = entries.findAll().stream().mapToLong(LedgerEntryEntity::signedAmount).sum();
        return total == 0L;
    }

    /** BR-LDG-05: refuses to post anything that does not balance — checked before any row is written. */
    private void post(List<Line> lines, String referenceType, UUID referenceId) {
        long balanceCheck = lines.stream().mapToLong(line -> line.debit() ? line.amount().minorUnits() : -line.amount().minorUnits()).sum();
        if (balanceCheck != 0) {
            throw new IllegalStateException(
                    "Unbalanced ledger posting for " + referenceType + " " + referenceId + ": off by " + balanceCheck);
        }

        Instant now = clock.instant();
        UUID postingId = UUID.randomUUID();

        for (Line line : lines) {
            entries.save(new LedgerEntryEntity(
                    UUID.randomUUID(),
                    postingId,
                    line.account(),
                    line.vendorId(),
                    line.debit(),
                    line.amount(),
                    referenceType,
                    referenceId,
                    now));
        }
    }
}
