package com.jamlapps.meridian.shared;

import java.io.InputStream;

/**
 * Content-addressed storage for uploaded files: vendor verification documents (BR-VND-03) and
 * product images (BR-CAT-05). Both need the same thing — accept bytes, hand back a reference,
 * retrieve by that reference later — so the abstraction lives here rather than being built twice.
 *
 * <p>Not the external-provider pattern of ADR-006 in the strict sense (nothing outside this process
 * is being called yet), but the same reasoning applies: INT-06 names object storage as a Critical
 * integration, and the default {@link LocalFilesystemBlobStorage} adapter exists so the platform is
 * buildable and testable before a GCS bucket and credentials exist. Swapping to a real bucket is
 * implementing this interface again, not changing any caller.
 */
public interface BlobStorage {

    /**
     * @param contentType stored alongside the bytes so {@link #retrieve} can report it; never
     *     inferred from the reference, which is opaque
     * @return an opaque reference. Callers persist this, never a filesystem path or URL — where and
     *     how it is stored is exactly what this interface exists to hide.
     */
    String store(InputStream content, String contentType, long contentLength);

    StoredBlob retrieve(String reference);

    record StoredBlob(InputStream content, String contentType, long contentLength) {}
}
