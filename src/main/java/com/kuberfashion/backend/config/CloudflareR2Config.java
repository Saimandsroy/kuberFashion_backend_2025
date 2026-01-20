package com.kuberfashion.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
@ConditionalOnProperty(name = "cloudflare.r2.enabled", havingValue = "true", matchIfMissing = false)
public class CloudflareR2Config {

    @Value("${cloudflare.r2.access-key:}")
    private String accessKey;

    @Value("${cloudflare.r2.secret-key:}")
    private String secretKey;

    @Value("${cloudflare.r2.account-id:}")
    private String accountId;

    @Value("${cloudflare.r2.bucket-name:kuberfashion}")
    private String bucketName;

    @Bean
    public S3Client s3Client() {
        if (accessKey == null || accessKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            return null; // R2 not configured
        }

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.US_EAST_1) // R2 uses auto region
                .endpointOverride(URI.create("https://" + accountId + ".r2.cloudflarestorage.com"))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    public String getBucketName() {
        return bucketName;
    }
}
