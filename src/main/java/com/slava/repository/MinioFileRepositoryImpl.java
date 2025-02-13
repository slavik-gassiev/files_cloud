package com.slava.repository;

import com.slava.exception.FileException;
import io.minio.*;
import io.minio.messages.Item;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class MinioFileRepositoryImpl implements CustomFileRepository {

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
            throw new FileException("Error uploading file: ");
        }
    }

    @Override
    public Optional<InputStream> downloadFile(String bucketName, String objectName) {
        try {
            return Optional.ofNullable(minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucketName).object(objectName).build()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public void deleteFile(String bucketName, String filePath) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(filePath).build());
        } catch (Exception e) {
            throw new FileException("Error deleting file: " + filePath);
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
            throw new FileException("Error copying file: " + sourceObjectName);
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
            throw new FileException("Error listing objects for prefix: ");
        }
    }

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            throw new FileException("Error checking bucket existence: " + bucketName);
        }
    }

    @Override
    public void createBucket(String bucketName) {
        try {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            throw new FileException("Error creating bucket: " + bucketName);
        }
    }

    @Override
    public void createFolder(String bucketName, String folderPath) {
        uploadFile(bucketName, folderPath.endsWith("/") ? folderPath : folderPath + "/",
                new ByteArrayInputStream(new byte[0]), 0, "application/x-directory");
    }

    @Override
    public void deleteFolder(String bucketName, String folderPath) {
        listObjects(bucketName, folderPath).forEach(obj -> deleteFile(bucketName, obj));
    }

    @Override
    public void moveFolder(String bucketName, String sourcePath, String targetPath) {
        listObjects(bucketName, sourcePath).forEach(obj -> {
            copyFile(bucketName, obj, targetPath + obj.substring(sourcePath.length()));
            deleteFile(bucketName, obj);
        });
    }

    @Override
    public void moveFile(String bucketName, String sourcePath, String targetPath) {
        copyFile(bucketName, sourcePath, targetPath);
        deleteFile(bucketName, sourcePath);
    }
}
