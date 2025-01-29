package com.slava.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;

@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint("http://localhost:9000")
                .credentials("minioadmin", "minioadmin")
                .build();
    }

    public void initializeUserRootFolder(MinioClient minioClient, String username) {
        try {
            String bucketName = username;

            // Проверяем, существует ли бакет с именем пользователя
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            // Если бакет не существует, создаем его
            if (!bucketExists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );

                // Создаем "корневую папку" (пустое содержимое)
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object("/")
                                .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                                .contentType("application/octet-stream")
                                .build()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Error initializing MinIO bucket for user: " + username, e);
        }
    }
}
