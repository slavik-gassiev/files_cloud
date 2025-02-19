package com.slava.service;

import com.slava.dto.*;
import com.slava.exception.FileException;
import com.slava.exception.FileNotFoundException;
import com.slava.repository.CustomFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private final CustomFileRepository fileRepository;

    public FileService(CustomFileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    public void moveFile(MoveFileDto fileOperationDto) {
        String newTargetPath = fileOperationDto.getTargetPath() + fileOperationDto.getFileName();
        log.info("Перемещение файла в бакете '{}': исходный путь '{}', новый путь '{}'",
                fileOperationDto.getBucketName(), fileOperationDto.getSourcePath(), newTargetPath);
        fileRepository.moveFile(fileOperationDto.getBucketName(), fileOperationDto.getSourcePath(), newTargetPath);
    }

    public void renameFile(RenameFileDto fileOperationDto) {
        String newTargetPath = fileOperationDto.getSourcePath().replace(fileOperationDto.getFileName(), fileOperationDto.getNewFileName());
        log.info("Переименование файла в бакете '{}': с '{}' на '{}'",
                fileOperationDto.getBucketName(), fileOperationDto.getFileName(), fileOperationDto.getNewFileName());
        fileRepository.moveFile(fileOperationDto.getBucketName(), fileOperationDto.getSourcePath(), newTargetPath);
    }

    public void deleteFile(DeleteFileDto deleteFileDto) {
        log.info("Удаление файла в бакете '{}', путь '{}'", deleteFileDto.getBucketName(), deleteFileDto.getSourcePath());
        fileRepository.deleteFile(deleteFileDto.getBucketName(), deleteFileDto.getSourcePath());
    }

    public byte[] downloadFile(String bucketName, String objectName) {
        log.debug("Загрузка файла из бакета '{}', объект '{}'", bucketName, objectName);
        return fileRepository.downloadFile(bucketName, objectName)
                .map(inputStream -> {
                    try {
                        byte[] bytes = inputStream.readAllBytes();
                        log.debug("Файл '{}' успешно загружен ({} байт)", objectName, bytes.length);
                        return bytes;
                    } catch (IOException e) {
                        log.error("Ошибка при чтении содержимого файла '{}': {}", objectName, e.getMessage(), e);
                        throw new FileException("Ошибка при загрузке файла");
                    }
                })
                .orElseThrow(() -> {
                    log.error("Файл '{}' не найден в бакете '{}'", objectName, bucketName);
                    return new FileNotFoundException("Файл не найден");
                });
    }

    public List<FileFolderDto> listFolderContents(String bucketName, String path) {
        String folderPath = (path == null || path.isEmpty()) ? "" : (path.endsWith("/") ? path : path + "/");
        log.debug("Получение содержимого папки в бакете '{}', путь '{}'", bucketName, folderPath);
        List<String> objects = fileRepository.listObjects(bucketName, folderPath);

        List<FileFolderDto> result = objects.stream()
                .filter(object -> {
                    String relativePath = object.substring(folderPath.length());
                    return !relativePath.isEmpty() &&
                            (!relativePath.contains("/") || relativePath.indexOf('/') == relativePath.length() - 1);
                })
                .map(object -> {
                    FileFolderDto dto = new FileFolderDto();
                    dto.setName(object.equals(folderPath) ? "Root" : object.substring(folderPath.length()));
                    dto.setPath(object);
                    dto.setFolder(object.endsWith("/"));
                    dto.setSize(0); // Размер для папок отсутствует, оставляем 0
                    return dto;
                })
                .toList();
        log.debug("Найдено {} элементов в папке '{}'", result.size(), folderPath);
        return result;
    }

    public void uploadFile(UploadFileDto uploadFileDto) {
        String objectName = uploadFileDto.getSourcePath() + uploadFileDto.getFileName();
        log.info("Загрузка файла '{}' в бакет '{}'", objectName, uploadFileDto.getBucketName());
        fileRepository.uploadFile(
                uploadFileDto.getBucketName(),
                objectName,
                new ByteArrayInputStream(uploadFileDto.getContent()),
                uploadFileDto.getContent().length,
                uploadFileDto.getContentType()
        );
    }

    public void ensureBucketExists(String bucketName) {
        log.info("Проверка существования бакета '{}'", bucketName);
        if (!fileRepository.bucketExists(bucketName)) {
            fileRepository.createBucket(bucketName);
        } else {
        }
    }

    public String getParentPathForFile(String fullPath) {
        log.debug("Определение родительского пути для '{}'", fullPath);
        int lastSlashIndex = fullPath.lastIndexOf("/");
        String parentPath = (lastSlashIndex != -1) ? fullPath.substring(0, lastSlashIndex) : "";
        return parentPath;
    }

    public List<String> getBreadcrumbLinks(String path) {
        log.debug("Генерация ссылок для пути '{}'", path);
        List<String> breadcrumbLinks = new ArrayList<>();
        StringBuilder fullPath = new StringBuilder();
        for (String segment : getPathSegments(path)) {
            if (!segment.isEmpty()) {
                fullPath.append(segment).append("/");
                breadcrumbLinks.add(fullPath.toString());
            }
        }
        return breadcrumbLinks;
    }

    public String[] getPathSegments(String path) {
        log.debug("Разбиение пути '{}' на сегменты", path);
        String[] pathSegments = path.isEmpty() ? new String[0] : path.split("/");
        return pathSegments;
    }
}
