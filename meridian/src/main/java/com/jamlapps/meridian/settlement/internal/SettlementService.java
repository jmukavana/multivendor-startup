package com.jamlapps.meridian.settlement.internal;

import com.jamlapps.meridian.ledger.LedgerPosting;
import com.jamlapps.meridian.ledger.TrialBalance;
import com.jamlapps.meridian.ledger.VendorLedger;
import com.jamlapps.meridian.ledger.VendorLedgerBalance;
import com.jamlapps.meridian.settlement.SettlementCycle;
import com.jamlapps.meridian.settlement.SettlementLine;
import com.jamlapps.meridian.settlement.SettlementLineStatus;
import com.jamlapps.meridian.settlement.SettlementRun;
import com.jamlapps.meridian.settlement.SettlementRunStatus;
import com.jamlapps.meridian.settlement.events.VendorDisbursed;
import com.jamlapps.meridian.vendor.Vendors;
import com.jamlapps.meridian.web.ApiException;
import com.jamlapps.meridian.web.AuditLog;
import com.jamlapps.meridian.web.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-STL-01 to 12. {@code disburseLine} is called from {@code SettlementController} and {@code
 * SettlementRetrySweeper} in a loop over line ids — never from another method in this class — so
 * that each call goes through this bean's Spring proxy and gets its own transaction, matching
 * {@code inventory.internal.ReservationExpirySweeper}'s reasoning exactly. Looping inside a method
 * of this class and calling {@code this.disburseLine(...)} would bypass the proxy: the atomic
 * {@code tryStartDisbursing} query needs an active transaction, and self-invocation would silently
 * run it without one.
 */
@Service
class SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);

    private final SettlementRunRepository runs;
    private final SettlementLineRepository lines;
    private final SettlementHoldRepository holds;
    private final SettlementCycleConfigurationRepository cycleConfig;
    private final VendorLedger vendorLedger;
    private final LedgerPosting ledgerPosting;
    private final TrialBalance trialBalance;
    private final Vendors vendors;
    private final DisbursementGateway gateway;
    private final ApplicationEventPublisher events;
    private final AuditLog auditLog;
    private final Clock clock;

    SettlementService(
            SettlementRunRepository runs,
            SettlementLineRepository lines,
            SettlementHoldRepository holds,
            SettlementCycleConfigurationRepository cycleConfig,
            VendorLedger vendorLedger,
            LedgerPosting ledgerPosting,
            TrialBalance trialBalance,
            Vendors vendors,
            DisbursementGateway gateway,
            ApplicationEventPublisher events,
            AuditLog auditLog,
            Clock clock) {
        this.runs = runs;
        this.lines = lines;
        this.holds = holds;
        this.cycleConfig = cycleConfig;
        this.vendorLedger = vendorLedger;
        this.ledgerPosting = ledgerPosting;
        this.trialBalance = trialBalance;
        this.vendors = vendors;
        this.gateway = gateway;
        this.events = events;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    /**
     * BR-STL-01/08/09/10, BR-ADM-06: reconciles first (AC-STL-02), then, if reconciled, calculates
     * one line per vendor currently owed a positive balance — excluded rather than included where
     * BR-STL-08's cooling-off or BR-STL-09's manual hold applies, where the vendor has no payout
     * details on file at all (its own exclusion reason, not "not in cooling off" — see {@code
     * Vendors.payoutCoolingOffUntil}'s javadoc), or where the configured settlement cycle has not
     * yet elapsed since that vendor's last disbursement (see {@link #isWithinSettlementCycleOfLastDisbursement}).
     */
    @Transactional
    SettlementRun initiate(Currency currency, UUID actorAccountId) {
        Instant now = clock.instant();

        if (!trialBalance.isReconciled()) {
            log.warn("Settlement run blocked: ledger did not reconcile to zero.");
            return runs.save(new SettlementRunEntity(UUID.randomUUID(), currency, SettlementRunStatus.BLOCKED, now))
                    .toSettlementRun();
        }

        SettlementRunEntity run = runs.save(new SettlementRunEntity(UUID.randomUUID(), currency, SettlementRunStatus.DRAFT, now));

        for (UUID vendorId : vendorLedger.vendorsWithPayableBalance(currency)) {
            VendorLedgerBalance balance = vendorLedger.balanceFor(vendorId, currency);
            String exclusionReason = exclusionReasonFor(vendorId, now);

            lines.save(exclusionReason == null
                    ? SettlementLineEntity.pending(UUID.randomUUID(), run.id(), vendorId, balance.payable())
                    : SettlementLineEntity.excluded(UUID.randomUUID(), run.id(), vendorId, balance.payable(), exclusionReason));
        }

        return run.toSettlementRun();
    }

    private @Nullable String exclusionReasonFor(UUID vendorId, Instant now) {
        if (holds.findByVendorIdAndReleasedAtIsNull(vendorId).isPresent()) {
            return "MANUAL_HOLD";
        }

        // Not Optional.map: a mapper returning null collapses to an empty Optional (Optional's own
        // documented behaviour), which would send "no payout details" and "cooling off already
        // passed" through the same .orElse branch — exactly backwards for the passed case. Checked
        // imperatively instead so "no exclusion" (null) stays distinguishable from "empty Optional".
        Optional<Instant> coolingOffUntil = vendors.payoutCoolingOffUntil(vendorId);
        if (coolingOffUntil.isEmpty()) {
            return "NO_PAYOUT_DETAILS";
        }
        if (now.isBefore(coolingOffUntil.get())) {
            return "PAYOUT_DETAILS_COOLING_OFF";
        }

        if (isWithinSettlementCycleOfLastDisbursement(vendorId, now)) {
            return "SETTLEMENT_CYCLE_NOT_ELAPSED";
        }

        return null;
    }

    /**
     * BR-ADM-06, OPEN-04: per-vendor, not a single lock on {@link #initiate}'s whole run — a fresh
     * vendor with no prior disbursement is never excluded by this, so this check cannot make a
     * vendor's very first settlement wait out a cycle that has not started for it yet. Resolves
     * the CURRENT cycle configuration at calculation time, deliberately, unlike commission and
     * holdback: those are captured once at order creation because COM-05 must reproduce what
     * applied to a specific historical order; a settlement cycle is not a per-order fact at all —
     * it is a scheduling policy evaluated fresh every time a run is calculated, the same as
     * BR-VND-09's cooling-off check just above it.
     */
    private boolean isWithinSettlementCycleOfLastDisbursement(UUID vendorId, Instant now) {
        return lines.findTopByVendorIdAndStatusOrderByResolvedAtDesc(vendorId, SettlementLineStatus.DISBURSED)
                .map(SettlementLineEntity::resolvedAt)
                .map(lastDisbursedAt -> now.isBefore(lastDisbursedAt.plus(currentCycleDays(), ChronoUnit.DAYS)))
                .orElse(false);
    }

    private int currentCycleDays() {
        // No fallback if this is empty: the same "fail loudly" reasoning as
        // pricing.internal.PricingConfigurationAdapter.currentCommissionRate. V34 seeds a default.
        return cycleConfig
                .findCurrent()
                .map(SettlementCycleConfigurationEntity::cycleDays)
                .orElseThrow(() -> new IllegalStateException("No settlement cycle configuration is in effect."));
    }

    /** BR-ADM-06: the same versioning discipline {@code pricing.PricingAdministration} follows, for the settlement cycle. */
    @Transactional
    SettlementCycle createCycleConfiguration(int cycleDays, Instant effectiveFrom, UUID adminAccountId) {
        if (cycleDays < 1 || cycleDays > 31) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR, "Settlement cycle must be between 1 (daily) and 31 (monthly) days.");
        }

        SettlementCycleConfigurationEntity entity = cycleConfig.save(
                new SettlementCycleConfigurationEntity(UUID.randomUUID(), cycleDays, effectiveFrom, clock.instant()));

        auditLog.record(
                adminAccountId,
                "SETTLEMENT_CYCLE_CONFIGURATION_CREATED",
                "SettlementCycleConfiguration",
                entity.id().toString(),
                "cycleDays=" + cycleDays + " effectiveFrom=" + effectiveFrom);

        return new SettlementCycle(entity.id(), entity.cycleDays());
    }

    /** BR-STL-11: the only transition into APPROVED; disbursement only ever follows this, never precedes it. */
    @Transactional
    SettlementRun approve(UUID runId, UUID actorAccountId) {
        SettlementRunEntity run = runs.findById(runId).orElseThrow(() -> ApiException.notFound("SettlementRun", runId));

        if (run.status() != SettlementRunStatus.DRAFT) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR, "Settlement run " + runId + " is not awaiting approval.");
        }

        run.approve(actorAccountId, clock.instant());
        runs.save(run);
        return run.toSettlementRun();
    }

    /**
     * BR-STL-12: claims the line atomically before doing anything else, so a retry that races
     * another in-flight attempt for the same line is a no-op, not a second disbursement. On
     * failure records the attempt and either leaves the line FAILED (retryable) or, past
     * MAX_ATTEMPTS, ESCALATED (BR-STL-07).
     *
     * <p>Calls the gateway before posting to the ledger — the same "acceptable simplification only
     * because the gateway is a mock" checkout's payment-initiation call already documents. A real
     * provider call here is network I/O inside this method's transaction, and a ledger-post failure
     * after the provider already paid would need reconciliation, not a rollback that pretends the
     * payment did not happen.
     */
    @Transactional
    void disburseLine(UUID lineId) {
        if (lines.tryStartDisbursing(lineId) == 0) {
            return;
        }

        SettlementLineEntity line = lines.findById(lineId).orElseThrow();
        Instant now = clock.instant();

        try {
            var result = gateway.disburse(line.vendorId(), line.amount(), line.idempotencyKey());
            ledgerPosting.postDisbursement(line.vendorId(), line.amount(), line.settlementRunId());
            line.recordSuccess(result.disbursementReference(), now);
            lines.save(line);

            events.publishEvent(new VendorDisbursed(
                    line.settlementRunId(), line.id(), line.vendorId(), line.amount(), result.disbursementReference(), now));
        } catch (RuntimeException e) {
            log.warn("Disbursement attempt failed for settlement line {}: {}", lineId, e.getMessage());
            line.recordFailure();
            lines.save(line);
        }

        completeRunIfFinished(line.settlementRunId());
    }

    private void completeRunIfFinished(UUID runId) {
        boolean anyUnresolved = lines.findBySettlementRunId(runId).stream().anyMatch(l -> l.status() == SettlementLineStatus.PENDING
                || l.status() == SettlementLineStatus.DISBURSING
                || l.status() == SettlementLineStatus.FAILED);

        if (!anyUnresolved) {
            runs.findById(runId).ifPresent(run -> {
                run.complete();
                runs.save(run);
            });
        }
    }

    @Transactional
    void hold(UUID vendorId, String reason, UUID actorAccountId) {
        if (!vendors.exists(vendorId)) {
            throw ApiException.notFound("Vendor", vendorId);
        }
        if (holds.findByVendorIdAndReleasedAtIsNull(vendorId).isPresent()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Vendor " + vendorId + " already has an active settlement hold.");
        }
        holds.save(new SettlementHoldEntity(UUID.randomUUID(), vendorId, reason, actorAccountId, clock.instant()));
    }

    @Transactional
    void releaseHold(UUID holdId, UUID actorAccountId) {
        SettlementHoldEntity hold = holds.findById(holdId).orElseThrow(() -> ApiException.notFound("SettlementHold", holdId));
        hold.release(actorAccountId, clock.instant());
        holds.save(hold);
    }

    /** BR-LDG-06 as vendors see it: owner-only or finance, matching payout details' own access rule. */
    @Transactional(readOnly = true)
    VendorLedgerBalance balanceFor(UUID vendorId, Currency currency, UUID callerAccountId, boolean callerIsFinance) {
        var vendor = vendors.findById(vendorId).orElseThrow(() -> ApiException.notFound("Vendor", vendorId));

        if (!vendor.ownerAccountId().equals(callerAccountId) && !callerIsFinance) {
            throw ApiException.notFound("Vendor", vendorId);
        }

        return vendorLedger.balanceFor(vendorId, currency);
    }

    @Transactional(readOnly = true)
    SettlementRun get(UUID runId) {
        return runs.findById(runId).map(SettlementRunEntity::toSettlementRun).orElseThrow(() -> ApiException.notFound("SettlementRun", runId));
    }

    @Transactional(readOnly = true)
    List<SettlementLine> listLines(UUID runId) {
        return lines.findBySettlementRunId(runId).stream().map(SettlementLineEntity::toSettlementLine).toList();
    }

    @Transactional(readOnly = true)
    List<UUID> pendingLineIds(UUID runId) {
        return lines.findBySettlementRunId(runId).stream()
                .filter(line -> line.status() == SettlementLineStatus.PENDING)
                .map(SettlementLineEntity::id)
                .toList();
    }

    @Transactional(readOnly = true)
    List<UUID> retryableLineIds() {
        return lines.findByStatus(SettlementLineStatus.FAILED).stream().map(SettlementLineEntity::id).toList();
    }
}
