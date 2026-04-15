package com.legal.document.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

@Configuration
@ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = false)
public class AmazonS3Config {

    @Value("${aws.access-key}")
    private String accessKey;

    @Value("${aws.secret-key}")
    private String secretKey;

    @Value("${aws.region}")
    private String region;

    private StaticCredentialsProvider credentials() {
        return StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKey, secretKey));
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(credentials())
            .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
            .region(Region.of(region))
            .credentialsProvider(credentials())
            .build();
    }

    @Bean
    public S3TransferManager transferManager() {
        return S3TransferManager.builder()
            .s3Client(S3AsyncClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials())
                .build())
            .build();
    }
}