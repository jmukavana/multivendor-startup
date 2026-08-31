package com.jamlapps.archfixture.alpha;

/** A type in alpha's API surface, referenced illegally by beta. */
public class AlphaInternals {
    public String value() {
        return "alpha";
    }
}
