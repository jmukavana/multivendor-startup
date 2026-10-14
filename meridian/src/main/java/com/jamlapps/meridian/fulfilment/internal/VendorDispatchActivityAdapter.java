package com.jamlapps.meridian.fulfilment.internal;

import com.jamlapps.meridian.fulfilment.VendorDispatchActivity;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class VendorDispatchActivityAdapter implements VendorDispatchActivity {

    private final DeliveryRepository deliveries;

    VendorDispatchActivityAdapter(DeliveryRepository deliveries) {
        this.deliveries = deliveries;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorDispatchStat> dispatchStats() {
        return deliveries.dispatchStatsByVendor().stream()
                .map(row -> new VendorDispatchStat(row.vendorId(), row.total(), row.failed()))
                .toList();
    }
}
