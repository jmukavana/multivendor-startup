package com.jamlapps.meridian.catalog.internal;

import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** BR-CAT-08, AC-CAT-03: case-insensitive substring match against the prohibited-items list. */
@Component
class ProhibitedTermChecker {

    private final ProhibitedTermRepository terms;

    ProhibitedTermChecker(ProhibitedTermRepository terms) {
        this.terms = terms;
    }

    /** @return the matched term, if the title contains one, so the caller can show it (AC-CAT-03) */
    Optional<String> firstMatch(String title) {
        String lowerTitle = title.toLowerCase(Locale.ROOT);
        return terms.findAll().stream()
                .map(ProhibitedTermEntity::term)
                .filter(term -> lowerTitle.contains(term.toLowerCase(Locale.ROOT)))
                .findFirst();
    }
}
