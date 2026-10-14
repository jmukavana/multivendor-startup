/**
 * Reviews and Ratings.
 *
 * <p>Implements BR-REV of the business requirements document. Delivered in phase 8.
 * Allowed dependencies: order, catalog, vendor.
 *
 * <p>All of BR-REV-01 to 06 are Should, not Must — the BRD re-baselines the whole module in
 * 10.12, on the reasoning that at launch scale trust is enforced administratively (BR-VND-10)
 * rather than through buyer reputation. But the same paragraph is explicit that BR-REV-01, 05 and
 * 06 are "not optional within them: an unverified review system is worse than none" — so this
 * module is built to that standard, not a reduced one, even though the module as a whole remains
 * a Should.
 *
 * <p>{@code vendor} is a deliberate addition beyond ADR-002's table, which lists only {@code
 * order} and {@code catalog} — matching the pattern {@code settlement}, {@code returns} and
 * {@code order} themselves already set. BR-REV-06 ("prevent a vendor from reviewing their own
 * products") and BR-REV-04 (a vendor responding to a review on their own product) both need to
 * resolve "does this account own the vendor behind this product", which only {@code vendor} can
 * answer.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Reviews and Ratings",
        allowedDependencies = { "order", "catalog", "vendor" })
package com.jamlapps.meridian.review;
