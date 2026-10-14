package com.jamlapps.meridian.tax.internal;

import com.jamlapps.meridian.tax.TaxDocumentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface TaxInvoiceRepository extends JpaRepository<TaxInvoiceEntity, UUID> {

    Optional<TaxInvoiceEntity> findByParentOrderId(UUID parentOrderId);

    /** REG-05: the retry sweep's query. */
    List<TaxInvoiceEntity> findByStatus(TaxDocumentStatus status);
}
