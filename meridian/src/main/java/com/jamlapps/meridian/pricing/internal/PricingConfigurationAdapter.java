package com.jamlapps.meridian.pricing.internal;

import com.jamlapps.meridian.pricing.CommissionRate;
import com.jamlapps.meridian.pricing.HoldbackRate;
import com.jamlapps.meridian.pricing.PricingConfiguration;
import com.jamlapps.meridian.web.ApiException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class PricingConfigurationAdapter implements PricingConfiguration {

    private final CommissionConfigurationRepository repository;
    private final HoldbackConfigurationRepository holdbackRepository;

    PricingConfigurationAdapter(CommissionConfigurationRepository repository, HoldbackConfigurationRepository holdbackRepository) {
        this.repository = repository;
        this.holdbackRepository = holdbackRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CommissionRate currentCommissionRate() {
        // No fallback if this is empty: an order that cannot resolve a commission rate must fail
        // loudly, not silently charge 0% or guess. V10 seeds a default, so this row always exists
        // in practice.
        CommissionConfigurationEntity current = repository
                .findCurrent()
                .orElseThrow(() -> new IllegalStateException("No commission configuration is in effect."));
        return new CommissionRate(current.id(), current.rate());
    }

    @Override
    @Transactional(readOnly = true)
    public CommissionRate rateForVersion(UUID configurationVersionId) {
        CommissionConfigurationEntity entity = repository
                .findById(configurationVersionId)
                .orElseThrow(() -> ApiException.notFound("Commission configuration", configurationVersionId));
        return new CommissionRate(entity.id(), entity.rate());
    }

    @Override
    @Transactional(readOnly = true)
    public HoldbackRate currentHoldbackRate() {
        // Same "fail loudly" reasoning as currentCommissionRate; V32 seeds a default row.
        HoldbackConfigurationEntity current = holdbackRepository
                .findCurrent()
                .orElseThrow(() -> new IllegalStateException("No holdback configuration is in effect."));
        return new HoldbackRate(current.id(), current.rate());
    }

    @Override
    @Transactional(readOnly = true)
    public HoldbackRate holdbackRateForVersion(UUID configurationVersionId) {
        HoldbackConfigurationEntity entity = holdbackRepository
                .findById(configurationVersionId)
                .orElseThrow(() -> ApiException.notFound("Holdback configuration", configurationVersionId));
        return new HoldbackRate(entity.id(), entity.rate());
    }
}
