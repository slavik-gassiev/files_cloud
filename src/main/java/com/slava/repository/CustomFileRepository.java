package com.slava.repository;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public interface CustomFileRepository {

    void uploadFile(String bucketName, String objectName, InputStream fileStream, long size, String contentType);

    Optional<InputStream> downloadFile(String bucketName, String objectName);

    void deleteFile(String bucketName, String objectName);

    void copyFile(String bucketName, String sourceObjectName, String targetObjectName);

    List<String> listObjects(String bucketName, String prefix);

    boolean bucketExists(String bucketName);

    void createBucket(String bucketName);
}

