package com.example.intranet_back_stage.security;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class StorageProperties {
    private String provider;     // minio | filesystem
    private String bucket;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private long urlTtlSeconds = 300;
    private String localRoot = "./data/docs";

    // getters/setters
    // ...
}
