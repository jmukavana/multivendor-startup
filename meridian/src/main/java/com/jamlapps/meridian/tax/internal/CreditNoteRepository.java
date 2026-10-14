package com.jamlapps.meridian.tax.internal;

import com.jamlapps.meridian.tax.TaxDocumentStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CreditNoteRepository extends JpaRepository<CreditNoteEntity, UUID> {

    /** REG-05: the retry sweep's query. */
    List<CreditNoteEntity> findByStatus(TaxDocumentStatus status);

    List<CreditNoteEntity> findByTaxInvoiceId(UUID taxInvoiceId);
}
