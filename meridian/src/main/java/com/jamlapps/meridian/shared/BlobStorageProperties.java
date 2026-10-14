package com.jamlapps.meridian.shared;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "meridian.blob-storage")
public record BlobStorageProperties(String directory) {

    public BlobStorageProperties {
        if (directory == null || directory.isBlank()) {
            directory = "/tmp/meridian-blob-storage";
        }
    }
}
