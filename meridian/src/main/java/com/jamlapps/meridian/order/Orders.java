package com.jamlapps.meridian.order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Orders {

    Optional<ParentOrder> findById(UUID parentOrderId);

    /** BR-ADM-02: the "by buyer" half of admin order search — most recent first, same ordering as the buyer's own order history. */
    List<ParentOrder> findByBuyerAccountId(UUID buyerAccountId);

    /** BR-ADM-02: the "by reference" half of admin order search. */
    Optional<ParentOrder> findByReference(String reference);

    /** BR-SEC-03: "sufficient to identify repeated activity from a single origin" — every order that shares one recorded source address. */
    List<ParentOrder> findBySourceAddress(String sourceAddress);

    /** BR-RTN-01/04/05: what {@code returns} needs to validate and price a return request. */
    Optional<SubOrderDetail> findSubOrder(UUID subOrderId);

    /** BR-REV-01: "only from buyers with a delivered order for that product" — the exact question {@code review} needs answered, nothing more. */
    boolean hasDeliveredProduct(UUID buyerAccountId, UUID productId);
}
