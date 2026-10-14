package com.jamlapps.meridian.pricing.internal;

import com.jamlapps.meridian.pricing.CommissionRate;
import com.jamlapps.meridian.pricing.HoldbackRate;
import com.jamlapps.meridian.pricing.PricingAdministration;
import com.jamlapps.meridian.web.ApiException;
import com.jamlapps.meridian.web.AuditLog;
import com.jamlapps.meridian.web.ErrorCode;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** BR-ADM-06: the real implementation behind {@code pricing.PricingAdministration}. */
@Service
class PricingService implements PricingAdministration {

    private final CommissionConfigurationRepository repository;
    private final HoldbackConfigurationRepository holdbackRepository;
    private final AuditLog auditLog;
    private final Clock clock;

    PricingService(
            CommissionConfigurationRepository repository,
            HoldbackConfigurationRepository holdbackRepository,
            AuditLog auditLog,
            Clock clock) {
        this.repository = repository;
        this.holdbackRepository = holdbackRepository;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CommissionRate createCommissionConfiguration(BigDecimal rate, Instant effectiveFrom, UUID adminAccountId) {
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.ONE) >= 0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Commission rate must be between 0 and 1 (exclusive).");
        }

        CommissionConfigurationEntity entity =
                repository.save(new CommissionConfigurationEntity(UUID.randomUUID(), rate, effectiveFrom, clock.instant()));

        auditLog.record(
                adminAccountId,
                "COMMISSION_CONFIGURATION_CREATED",
                "CommissionConfiguration",
                entity.id().toString(),
                "rate=" + rate + " effectiveFrom=" + effectiveFrom);

        return new CommissionRate(entity.id(), entity.rate());
    }

    @Override
    @Transactional
    public HoldbackRate createHoldbackConfiguration(BigDecimal rate, Instant effectiveFrom, UUID adminAccountId) {
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.ONE) >= 0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Holdback rate must be between 0 and 1 (exclusive).");
        }

        HoldbackConfigurationEntity entity =
                holdbackRepository.save(new HoldbackConfigurationEntity(UUID.randomUUID(), rate, effectiveFrom, clock.instant()));

        auditLog.record(
                adminAccountId,
                "HOLDBACK_CONFIGURATION_CREATED",
                "HoldbackConfiguration",
                entity.id().toString(),
                "rate=" + rate + " effectiveFrom=" + effectiveFrom);

        return new HoldbackRate(entity.id(), entity.rate());
    }
}
