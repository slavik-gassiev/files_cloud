package com.slava.repository;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public interface CustomFileRepository {

    void uploadFile(String objectName, InputStream fileStream, long size, String contentType);

    Optional<InputStream> downloadFile(String objectName);

    void deleteFile(String objectName);

    void copyFile(String sourceObjectName, String targetObjectName);

    List<String> listObjects(String prefix);
}

