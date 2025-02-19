package com.slava.repository;

import com.slava.exception.FileException;
import io.minio.*;
import io.minio.messages.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class MinioFileRepositoryImpl implements CustomFileRepository {

    private static final Logger log = LoggerFactory.getLogger(MinioFileRepositoryImpl.class);

    private final MinioClient minioClient;

    public MinioFileRepositoryImpl(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public void uploadFile(String bucketName, String objectName, InputStream fileStream, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(fileStream, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            log.error("Ошибка при загрузке файла '{}' в бакет '{}': {}", objectName, bucketName, e.getMessage(), e);
            throw new FileException("Ошибка при загрузке файла");
        }
    }

    @Override
    public Optional<InputStream> downloadFile(String bucketName, String objectName) {
        try {
            return Optional.ofNullable(minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucketName).object(objectName).build()));
        } catch (Exception e) {
            log.error("Ошибка при загрузке файла '{}' из бакета '{}': {}", objectName, bucketName, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public void deleteFile(String bucketName, String filePath) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(filePath).build());
        } catch (Exception e) {
            log.error("Ошибка при удалении файла '{}' из бакета '{}': {}", filePath, bucketName, e.getMessage(), e);
            throw new FileException("Ошибка при удалении файла");
        }
    }

    @Override
    public void copyFile(String bucketName, String sourceObjectName, String targetObjectName) {
        try {
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(bucketName)
                    .object(targetObjectName)
                    .source(CopySource.builder().bucket(bucketName).object(sourceObjectName).build())
                    .build());
        } catch (Exception e) {
            log.error("Ошибка при копировании файла '{}' в бакете '{}': {}", sourceObjectName, bucketName, e.getMessage(), e);
            throw new FileException("Ошибка при копировании файла");
        }
    }

    @Override
    public List<String> listObjects(String bucketName, String prefix) {
        try {
            List<String> objectNames = new ArrayList<>();
            for (Result<Item> result : minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .recursive(true)
                    .build())) {
                objectNames.add(result.get().objectName());
            }
            return objectNames;
        } catch (Exception e) {
            log.error("Ошибка при получении списка объектов в бакете '{}' с префиксом '{}': {}", bucketName, prefix, e.getMessage(), e);
            throw new FileException("Ошибка при получении списка файлов");
        }
    }

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            return exists;
        } catch (Exception e) {
            log.error("Ошибка при проверке существования бакета '{}': {}", bucketName, e.getMessage(), e);
            throw new FileException("Ошибка при проверке существования бакета: " + bucketName);
        }
    }

    @Override
    public void createBucket(String bucketName) {
        try {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            log.error("Ошибка при создании бакета '{}': {}", bucketName, e.getMessage(), e);
            throw new FileException("Ошибка при создании бакета: " + bucketName);
        }
    }

    @Override
    public void createFolder(String bucketName, String folderPath) {
        try {
            uploadFile(bucketName, folderPath.endsWith("/") ? folderPath : folderPath + "/",
                    new ByteArrayInputStream(new byte[0]), 0, "application/x-directory");
        } catch (Exception e) {
            log.error("Ошибка при создании папки '{}' в бакете '{}': {}", folderPath, bucketName, e.getMessage(), e);
            throw new FileException("Ошибка при создании папки");
        }
    }

    @Override
    public void deleteFolder(String bucketName, String folderPath) {
        log.info("Удаление папки '{}' из бакета '{}'", folderPath, bucketName);
        listObjects(bucketName, folderPath).forEach(obj -> {
            deleteFile(bucketName, obj);
        });
    }

    @Override
    public void moveFolder(String bucketName, String sourcePath, String targetPath) {
        log.info("Перемещение папки в бакете '{}': с '{}' на '{}'", bucketName, sourcePath, targetPath);
        listObjects(bucketName, sourcePath).forEach(obj -> {
            String newTarget = targetPath + obj.substring(sourcePath.length());
            copyFile(bucketName, obj, newTarget);
            deleteFile(bucketName, obj);
        });
    }

    @Override
    public void moveFile(String bucketName, String sourcePath, String targetPath) {
        copyFile(bucketName, sourcePath, targetPath);
        deleteFile(bucketName, sourcePath);
        log.info("Файл успешно перемещён в бакете '{}'", bucketName);
    }
}
