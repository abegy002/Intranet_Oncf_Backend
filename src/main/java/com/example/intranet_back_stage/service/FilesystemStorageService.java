package com.example.intranet_back_stage.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.*;
import java.time.Duration;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "filesystem")
@RequiredArgsConstructor
public class FilesystemStorageService implements StorageService {

    @Value("${storage.localRoot:./data/docs}")
    private String root;

    private Path rootPath() throws Exception {
        Path p = Paths.get(root).toAbsolutePath().normalize();
        Files.createDirectories(p);
        return p;
    }

    @Override
    public String put(InputStream in, long size, String contentType, String keyHint) throws Exception {
        // generate a unique key, but keep a readable hint/prefix
        String key = (keyHint == null ? "doc" : keyHint) + "_" + UUID.randomUUID();

        Path root = rootPath();
        Path target = root.resolve(key).normalize();

        // safety: prevent escaping the root with ".."
        if (!target.startsWith(root)) {
            throw new SecurityException("Invalid storage key path");
        }

        Files.createDirectories(target.getParent());
        try (OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
            in.transferTo(out);
        }
        return key;
    }

    @Override
    public URL getSignedUrl(String storageKey, Duration ttl) throws Exception {
        // For filesystem, a "file:" URL isn't useful to browsers across machines.
        // Keep this for compatibility, but consider streaming via a controller.
        Path p = rootPath().resolve(storageKey).normalize();
        if (!p.startsWith(rootPath())) throw new SecurityException("Invalid storage key path");
        return new URL("file:" + p.toString());
    }

    @Override
    public InputStream get(String storageKey) throws Exception {
        Path p = rootPath().resolve(storageKey).normalize();
        if (!p.startsWith(rootPath())) throw new SecurityException("Invalid storage key path");
        return Files.newInputStream(p, StandardOpenOption.READ);
    }

    @Override
    public void delete(String key) throws Exception {
        Path root = rootPath();
        Path target = root.resolve(key).normalize();

        // safety: keep within root
        if (!target.startsWith(root)) {
            throw new SecurityException("Invalid storage key path");
        }

        // delete the file if it exists
        Files.deleteIfExists(target);

        // optional: prune empty parent directories back up to root
        Path parent = target.getParent();
        while (parent != null && !parent.equals(root)) {
            try (var dirStream = Files.newDirectoryStream(parent)) {
                if (dirStream.iterator().hasNext()) break; // not empty
            }
            try {
                Files.deleteIfExists(parent);
            } catch (Exception ignored) {
                // if another thread creates something here, ignore and stop cleaning
                break;
            }
            parent = parent.getParent();
        }
    }
}
