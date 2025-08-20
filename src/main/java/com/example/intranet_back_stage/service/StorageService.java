package com.example.intranet_back_stage.service;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;

public interface StorageService {
    String put(InputStream in, long size, String contentType, String keyHint) throws Exception;
    URL getSignedUrl(String storageKey, Duration ttl) throws Exception;
    InputStream get(String storageKey) throws Exception;
    void delete(String key) throws Exception;
}
