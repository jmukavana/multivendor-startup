/**
 * Search and Discovery.
 *
 * <p>Implements BR-SCH of the business requirements document. Delivered in phase 8.
 * Allowed dependencies: catalog.
 *
 * <p>Matches ADR-002's table exactly — no deviation either way, the same as {@code fulfilment}.
 * That is a deliberate scope choice, not an accident: two of BR-SCH-02's five filter dimensions
 * (rating, delivery availability) and part of BR-SCH-04 (out-of-stock exclusion) need {@code
 * review}, {@code fulfilment} and {@code inventory} respectively, none of which are dependencies
 * here. Rather than add them opportunistically because the data happens to exist elsewhere, this
 * pass keeps {@code search}'s dependency exactly as ADR-002 states and defers those three pieces
 * explicitly — see {@code catalog.ProductSearch}'s javadoc and {@code SearchService}'s own notes on
 * each. Category filtering is not deferred so much as genuinely blocked: BR-CAT-03 (category
 * taxonomy) does not exist anywhere in {@code catalog} yet for anything to filter by.
 *
 * <p>{@code catalog.ProductSearch} is the query primitive this module is built on — BR-SCH-01's
 * keyword matching, BR-SCH-02's price/vendor filters, BR-SCH-03's price/recency/relevance sorts,
 * and BR-SCH-04's active-only default all resolve there. This module's own value is BR-SCH-06's
 * zero-result logging and being the one place a `search` experience — rather than a `catalog`
 * concern — lives, so that adding a second data source later (a rating filter once `review` is
 * wired in, say) is a change to this module, not to {@code catalog}'s own query surface.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Search and Discovery",
        allowedDependencies = { "catalog" })
package com.jamlapps.meridian.search;
