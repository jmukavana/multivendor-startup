package com.jamlapps.meridian.iam.internal;

import org.springframework.stereotype.Component;

@Component
class DefaultOneTimeCodeGenerator implements OneTimeCodeGenerator {

    @Override
    public String generate() {
        return TokenHashing.generateOneTimeCode();
    }
}
