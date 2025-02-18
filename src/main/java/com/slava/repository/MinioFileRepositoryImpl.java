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
            log.debug("Загрузка файла '{}' в бакет '{}'", objectName, bucketName);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(fileStream, size, -1)
                    .contentType(contentType)
                    .build());
            log.info("Файл '{}' успешно загружен в бакет '{}'", objectName, bucketName);
        } catch (Exception e) {
            log.error("Ошибка при загрузке файла '{}' в бакет '{}': {}", objectName, bucketName, e.getMessage(), e);
            throw new FileException("Ошибка при загрузке файла");
        }
    }

    @Override
    public Optional<InputStream> downloadFile(String bucketName, String objectName) {
        try {
            log.debug("Загрузка файла '{}' из бакета '{}'", objectName, bucketName);
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
            log.info("Удаление файла '{}' из бакета '{}'", filePath, bucketName);
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(filePath).build());
            log.info("Файл '{}' успешно удалён из бакета '{}'", filePath, bucketName);
        } catch (Exception e) {
            log.error("Ошибка при удалении файла '{}' из бакета '{}': {}", filePath, bucketName, e.getMessage(), e);
            throw new FileException("Ошибка при удалении файла");
        }
    }

    @Override
    public void copyFile(String bucketName, String sourceObjectName, String targetObjectName) {
        try {
            log.info("Копирование файла '{}' в '{}' в бакете '{}'", sourceObjectName, targetObjectName, bucketName);
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(bucketName)
                    .object(targetObjectName)
                    .source(CopySource.builder().bucket(bucketName).object(sourceObjectName).build())
                    .build());
            log.info("Файл '{}' успешно скопирован в '{}' в бакете '{}'", sourceObjectName, targetObjectName, bucketName);
        } catch (Exception e) {
            log.error("Ошибка при копировании файла '{}' в бакете '{}': {}", sourceObjectName, bucketName, e.getMessage(), e);
            throw new FileException("Ошибка при копировании файла");
        }
    }

    @Override
    public List<String> listObjects(String bucketName, String prefix) {
        try {
            log.debug("Получение списка объектов в бакете '{}' с префиксом '{}'", bucketName, prefix);
            List<String> objectNames = new ArrayList<>();
            for (Result<Item> result : minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .recursive(true)
                    .build())) {
                objectNames.add(result.get().objectName());
            }
            log.info("Найдено {} объектов в бакете '{}' с префиксом '{}'", objectNames.size(), bucketName, prefix);
            return objectNames;
        } catch (Exception e) {
            log.error("Ошибка при получении списка объектов в бакете '{}' с префиксом '{}': {}", bucketName, prefix, e.getMessage(), e);
            throw new FileException("Ошибка при получении списка файлов");
        }
    }

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            log.debug("Проверка существования бакета '{}'", bucketName);
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            log.info("Бакет '{}' существует: {}", bucketName, exists);
            return exists;
        } catch (Exception e) {
            log.error("Ошибка при проверке существования бакета '{}': {}", bucketName, e.getMessage(), e);
            throw new FileException("Ошибка при проверке существования бакета: " + bucketName);
        }
    }

    @Override
    public void createBucket(String bucketName) {
        try {
            log.info("Создание бакета '{}'", bucketName);
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("Бакет '{}' успешно создан", bucketName);
        } catch (Exception e) {
            log.error("Ошибка при создании бакета '{}': {}", bucketName, e.getMessage(), e);
            throw new FileException("Ошибка при создании бакета: " + bucketName);
        }
    }

    @Override
    public void createFolder(String bucketName, String folderPath) {
        try {
            log.info("Создание папки '{}' в бакете '{}'", folderPath, bucketName);
            uploadFile(bucketName, folderPath.endsWith("/") ? folderPath : folderPath + "/",
                    new ByteArrayInputStream(new byte[0]), 0, "application/x-directory");
            log.info("Папка '{}' успешно создана в бакете '{}'", folderPath, bucketName);
        } catch (Exception e) {
            log.error("Ошибка при создании папки '{}' в бакете '{}': {}", folderPath, bucketName, e.getMessage(), e);
            throw new FileException("Ошибка при создании папки");
        }
    }

    @Override
    public void deleteFolder(String bucketName, String folderPath) {
        log.info("Удаление папки '{}' из бакета '{}'", folderPath, bucketName);
        listObjects(bucketName, folderPath).forEach(obj -> {
            log.debug("Удаление объекта '{}' в рамках удаления папки", obj);
            deleteFile(bucketName, obj);
        });
        log.info("Папка '{}' успешно удалена из бакета '{}'", folderPath, bucketName);
    }

    @Override
    public void moveFolder(String bucketName, String sourcePath, String targetPath) {
        log.info("Перемещение папки в бакете '{}': с '{}' на '{}'", bucketName, sourcePath, targetPath);
        listObjects(bucketName, sourcePath).forEach(obj -> {
            String newTarget = targetPath + obj.substring(sourcePath.length());
            log.debug("Копирование объекта '{}' в '{}'", obj, newTarget);
            copyFile(bucketName, obj, newTarget);
            log.debug("Удаление оригинального объекта '{}'", obj);
            deleteFile(bucketName, obj);
        });
        log.info("Папка успешно перемещена в бакете '{}'", bucketName);
    }

    @Override
    public void moveFile(String bucketName, String sourcePath, String targetPath) {
        log.info("Перемещение файла в бакете '{}': с '{}' на '{}'", bucketName, sourcePath, targetPath);
        copyFile(bucketName, sourcePath, targetPath);
        deleteFile(bucketName, sourcePath);
        log.info("Файл успешно перемещён в бакете '{}'", bucketName);
    }
}
