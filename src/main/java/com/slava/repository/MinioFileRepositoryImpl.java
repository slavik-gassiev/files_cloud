package com.slava.repository;

import io.minio.*;
import io.minio.messages.Item;
import org.springframework.stereotype.Repository;

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
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(fileStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error uploading file: " + objectName, e);
        }
    }

    @Override
    public Optional<InputStream> downloadFile(String bucketName, String objectName) {
        try {
            InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return Optional.ofNullable(inputStream);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public void deleteFile(String bucketName, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error deleting file: " + objectName, e);
        }
    }

    @Override
    public void copyFile(String bucketName, String sourceObjectName, String targetObjectName) {
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(targetObjectName)
                            .source(CopySource.builder()
                                    .bucket(bucketName)
                                    .object(sourceObjectName)
                                    .build())
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error copying file from " + sourceObjectName + " to " + targetObjectName, e);
        }
    }

    @Override
    public List<String> listObjects(String bucketName, String prefix) {
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .recursive(true)
                            .build()
            );

            List<String> objectNames = new ArrayList<>();
            for (Result<Item> result : results) {
                objectNames.add(result.get().objectName());
            }
            return objectNames;
        } catch (Exception e) {
            throw new RuntimeException("Error listing objects with prefix: " + prefix, e);
        }
    }

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            throw new RuntimeException("Error checking if bucket exists: " + bucketName, e);
        }
    }

    @Override
    public void createBucket(String bucketName) {
        try {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            throw new RuntimeException("Error creating bucket: " + bucketName, e);
        }
    }

}

