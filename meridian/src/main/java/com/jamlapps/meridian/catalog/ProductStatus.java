package com.jamlapps.meridian.catalog;

/**
 * A reduced set against BR-CAT-06's full six states (Draft, Pending Review, Active, Out of Stock,
 * Delisted, Rejected). Kept deliberately small for Phase 2:
 *
 * <ul>
 *   <li>{@code Pending Review} does not exist — AC-CAT-03's prohibited-term check runs
 *       synchronously at publish time and either blocks it or does not; there is no moderation
 *       queue for a human to review a listing before it goes live.
 *   <li>{@code Out of Stock} does not exist — that is {@code inventory}'s concern (BR-INV), not
 *       modelled yet.
 *   <li>{@code Rejected} does not exist — nothing in Phase 2 rejects a product after submission;
 *       the prohibited-term check at publish either passes or refuses outright, it does not leave
 *       a rejected record behind.
 * </ul>
 */
public enum ProductStatus {

    /** Created, not yet publishable-checked. May have no image, may not have been price-checked. */
    DRAFT,

    /** Published: at least one image, passed the prohibited-term check (BR-CAT-05, BR-CAT-08). */
    ACTIVE,

    DELISTED
}
