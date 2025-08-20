package com.example.intranet_back_stage.service;

import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "minio")
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private final MinioClient client;

    @Value("${storage.bucket}") private String bucket;
    @Value("${storage.urlTtlSeconds:300}") private long defaultTtlSeconds;

    /**
     * If keyHint is provided, we now use it as the FINAL object key (no UUID suffix).
     * This lets callers control folder/name layout in MinIO.
     */
    @Override
    public String put(InputStream in, long size, String contentType, String keyHint) throws Exception {
        String objectKey = (keyHint == null || keyHint.isBlank())
                ? ("docs/" + UUID.randomUUID())  // fallback
                : keyHint;

        client.putObject(
                PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectKey)
                        .stream(in, size, -1)
                        .contentType(contentType != null ? contentType : "application/octet-stream")
                        .build()
        );
        return objectKey;
    }

    @Override
    public URL getSignedUrl(String storageKey, Duration ttl) throws Exception {
        int expiry = (int) (ttl != null ? ttl.getSeconds() : defaultTtlSeconds);
        String presigned = client.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(bucket)
                        .object(storageKey)
                        .method(Method.GET)
                        .expiry(expiry)
                        .build()
        );
        return new URL(presigned);
    }

    @Override
    public InputStream get(String storageKey) throws Exception {
        return client.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(storageKey)
                        .build()
        );
    }

    @Override
    public void delete(String key) throws Exception {
        client.removeObject(
                io.minio.RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(key)
                        .build()
        );
    }
}
