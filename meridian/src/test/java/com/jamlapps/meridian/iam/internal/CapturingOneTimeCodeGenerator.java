package com.jamlapps.meridian.iam.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Primary;

/**
 * Test double for {@link OneTimeCodeGenerator} that still produces real random codes — nothing
 * about hashing or verification is faked — but also records the plaintext, keyed by the account it
 * was generated for, so a test can retrieve what {@link AuthenticationService} would otherwise
 * never expose.
 *
 * <p>{@code @Primary} so it replaces {@link DefaultOneTimeCodeGenerator} wherever it is imported.
 * Lives in this package, not in test-support shared across modules, because reaching for it from
 * outside {@code iam} would be exactly the kind of internal access {@code ModularityTests} exists
 * to catch — even from a test.
 */
@TestComponent
@Primary
class CapturingOneTimeCodeGenerator implements OneTimeCodeGenerator {

    private final Map<Long, String> byThread = new ConcurrentHashMap<>();

    @Override
    public String generate() {
        String code = TokenHashing.generateOneTimeCode();
        // Keyed by thread rather than by account: generation and capture happen inside the same
        // request thread and there is no account identifier available at this seam — accounts is
        // resolved by AuthenticationService, one layer up. A request-scoped capture is exactly
        // what "the code just generated on this thread" means for a synchronous MockMvc call.
        byThread.put(Thread.currentThread().threadId(), code);
        return code;
    }

    String mostRecentlyGenerated() {
        String code = byThread.get(Thread.currentThread().threadId());
        if (code == null) {
            throw new IllegalStateException(
                    "No code was generated on this thread. Call register() or requestCode() first.");
        }
        return code;
    }
}
