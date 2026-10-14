package com.jamlapps.meridian.ledger.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface LedgerEntryRepository extends JpaRepository<LedgerEntryEntity, UUID> {

    List<LedgerEntryEntity> findByVendorIdAndAccount(UUID vendorId, LedgerAccount account);

    // BR-LDG-05's trial balance reads every entry — JpaRepository already provides findAll() for
    // that; no need to redeclare it here.

    /**
     * BR-STL-01: every vendor with a strictly positive VENDOR_PAYABLE balance, summed in the
     * database rather than in Java — pulling every entry for every vendor into memory just to
     * filter most of them back out would not scale past a trivial number of vendors.
     */
    @Query("select e.vendorId from LedgerEntryEntity e where e.account = com.jamlapps.meridian.ledger.internal.LedgerAccount.VENDOR_PAYABLE "
            + "and e.currency = :currency "
            + "group by e.vendorId "
            + "having sum(case when e.debit then -e.amountMinorUnits else e.amountMinorUnits end) > 0")
    List<UUID> vendorIdsWithPositivePayableBalance(String currency);
}
