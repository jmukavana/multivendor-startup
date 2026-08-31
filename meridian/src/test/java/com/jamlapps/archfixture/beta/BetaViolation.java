package com.jamlapps.archfixture.beta;

import com.jamlapps.archfixture.alpha.AlphaInternals;

/**
 * The violation itself: beta declares no allowed dependencies but reaches into alpha.
 * This must not be "fixed". The build proves boundary enforcement works by detecting it.
 */
public class BetaViolation {
    public String leak() {
        return new AlphaInternals().value();
    }
}
