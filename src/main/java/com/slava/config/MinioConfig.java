package com.slava.config;

import io.minio.*;
import io.minio.messages.Item;
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
            }

            // Создаем "корневую папку" (папка внутри бакета)
            String rootFolder = "root/";

            // Проверяем, есть ли уже объект с этим именем
            Iterable<Result<Item>> objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(rootFolder)
                            .build()
            );

            if (!objects.iterator().hasNext()) {
                // Создаем "пустой" объект, имитирующий папку
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(rootFolder) // Имя "папки"
                                .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                                .contentType("application/octet-stream")
                                .build()
                );
            }

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при инициализации MinIO для пользователя: " + username, e);
        }
    }
}

