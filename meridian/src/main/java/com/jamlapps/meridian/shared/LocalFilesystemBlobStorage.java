package com.jamlapps.meridian.shared;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Development and staging default: files on local disk. Not appropriate once more than one
 * replica runs, since a file written by one pod is invisible to another — see
 * {@code deploy/staging/README.md}. Kept as the default specifically because it needs no bucket, no
 * credentials and no network to exist, which is what lets document and image upload be built and
 * tested before INT-06 has a real provider behind it.
 */
@Component
class LocalFilesystemBlobStorage implements BlobStorage {

    private final Path root;

    LocalFilesystemBlobStorage(BlobStorageProperties properties) {
        this.root = Path.of(properties.directory());
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create blob storage directory: " + root, e);
        }
    }

    @Override
    public String store(InputStream content, String contentType, long contentLength) {
        String reference = UUID.randomUUID().toString();
        Path target = fileFor(reference);
        Path metadata = metadataFor(reference);

        try {
            Files.copy(content, target);
            Files.writeString(metadata, contentType);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store blob " + reference, e);
        }

        return reference;
    }

    @Override
    public StoredBlob retrieve(String reference) {
        Path target = fileFor(validated(reference));
        Path metadata = metadataFor(reference);

        try {
            String contentType = Files.readString(metadata);
            long length = Files.size(target);
            return new StoredBlob(Files.newInputStream(target), contentType, length);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not retrieve blob " + reference, e);
        }
    }

    private Path fileFor(String reference) {
        return root.resolve(reference + ".bin");
    }

    private Path metadataFor(String reference) {
        return root.resolve(reference + ".type");
    }

    /**
     * References become part of a filesystem path. A reference is always one this class generated
     * itself (a random UUID), but this guards against that invariant ever being violated —
     * accepting an attacker-influenced reference unchecked here would be a path traversal.
     */
    private static String validated(String reference) {
        if (!reference.matches("[0-9a-fA-F-]{36}")) {
            throw new IllegalArgumentException("Not a valid blob reference: " + reference);
        }
        return reference;
    }
}
