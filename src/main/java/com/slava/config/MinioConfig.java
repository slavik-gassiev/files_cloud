package com.slava.config;

import io.minio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    @Bean
    public MinioClient minioClient() {
        log.info("Создание MinIO клиента с endpoint {}", "http://localhost:9100");
        return MinioClient.builder()
                .endpoint("http://localhost:9100")
                .credentials("minioadmin", "minioadmin")
                .build();
    }

    public void initializeUserRootFolder(MinioClient minioClient, String username) {
        try {
            String bucketName = username;
            log.info("Инициализация корневой папки пользователя '{}', бакет '{}'", username, bucketName);

            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );
            log.debug("Проверка существования бакета '{}': {}", bucketName, bucketExists);

            if (!bucketExists) {
                log.info("Бакет '{}' не найден. Создаю бакет...", bucketName);
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
                log.info("Бакет '{}' успешно создан", bucketName);
            } else {
                log.info("Бакет '{}' уже существует", bucketName);
            }
        } catch (Exception e) {
            log.error("Ошибка при инициализации MinIO для пользователя '{}': {}", username, e.getMessage(), e);
            throw new RuntimeException("Ошибка при инициализации MinIO для пользователя: " + username, e);
        }
    }
}
