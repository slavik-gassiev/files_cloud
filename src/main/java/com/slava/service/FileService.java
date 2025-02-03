package com.slava.service;

import com.slava.dto.FileFolderDto;
import com.slava.dto.FileOperationDto;
import com.slava.repository.CustomFileRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@Service
public class FileService {

    private final CustomFileRepository fileRepository;

    public FileService(CustomFileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    public void createFolder(String bucketName, String path) {
        fileRepository.createFolder(bucketName, path);
    }

    public void deleteFolder(String bucketName, String path) {
        fileRepository.deleteFolder(bucketName, path);
    }

    public void renameFolder(FileOperationDto fileOperationDto) {
        moveFolder(fileOperationDto);
    }

    public void moveFolder(FileOperationDto fileOperationDto) {
        String folderName = fileOperationDto.getSourcePath();
        if (folderName.endsWith("/")) {
            folderName = folderName.substring(0, folderName.length() - 1); // Убираем последний слэш
        }
        folderName = folderName.substring(folderName.lastIndexOf("/") + 1); // Извлекаем только имя папки

        String targetPath = fileOperationDto.getTargetPath();
        if (!targetPath.endsWith("/")) {
            targetPath += "/";
        }

        String targetPathWithFolderName = targetPath + folderName;

        fileRepository.moveFolder(fileOperationDto.getBucketName(), fileOperationDto.getSourcePath(), targetPathWithFolderName);
    }



    public void moveFile(FileOperationDto fileOperationDto) {
        String fileName = fileOperationDto.getSourcePath().substring(fileOperationDto.getSourcePath().lastIndexOf("/") + 1);
        String targetPathWithFileName = fileOperationDto.getTargetPath() + fileName;
        fileRepository.moveFile(fileOperationDto.getBucketName(), fileOperationDto.getSourcePath(), targetPathWithFileName);
    }

    public List<FileFolderDto> listFolderContents(String bucketName, String path) {
        String folderPath = (path == null || path.isEmpty()) ? "" : (path.endsWith("/") ? path : path + "/");
        List<String> objects = fileRepository.listObjects(bucketName, folderPath);

        return objects.stream()
                .filter(object -> {
                    String relativePath = object.substring(folderPath.length());
                    // Фильтруем элементы на текущем уровне: либо файлы, либо папки без дополнительных подуровней
                    return !relativePath.isEmpty() &&
                            (!relativePath.contains("/") || relativePath.indexOf('/') == relativePath.length() - 1);
                })
                .map(object -> {
                    FileFolderDto dto = new FileFolderDto();
                    dto.setName(object.equals(folderPath) ? "Root" : object.substring(folderPath.length()));
                    dto.setPath(object);
                    dto.setFolder(object.endsWith("/"));
                    dto.setSize(0); // MinIO не возвращает размер для папок, оставляем 0
                    return dto;
                })
                .toList();
    }

    public void uploadFile(String bucketName, String objectName, byte[] content, String contentType) {
        fileRepository.uploadFile(bucketName, objectName, new ByteArrayInputStream(content), content.length, contentType);
    }

    public byte[] downloadFile(String bucketName, String objectName) {
        return fileRepository.downloadFile(bucketName, objectName)
                .map(inputStream -> {
                    try {
                        return inputStream.readAllBytes();
                    } catch (IOException e) {
                        throw new RuntimeException("Error reading file content", e);
                    }
                })
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
    }

    public List<FileFolderDto> listOnlyFolders(String bucketName) {
        List<String> objects = fileRepository.listObjects(bucketName, "");

        // Фильтруем только папки
        return objects.stream()
                .filter(object -> object.endsWith("/")) // Сохраняем только пути, заканчивающиеся на "/"
                .map(object -> {
                    FileFolderDto dto = new FileFolderDto();
                    dto.setName(object); // Имя папки без полного пути
                    dto.setPath(object);
                    dto.setFolder(true);
                    dto.setSize(0); // Размер для папок отсутствует
                    return dto;
                })
                .toList();
    }

    public void ensureBucketExists(String bucketName) {
        if (!fileRepository.bucketExists(bucketName)) {
            fileRepository.createBucket(bucketName);
        }
    }

    public void deleteFile(String bucketName, String objectName) {
        fileRepository.deleteFile(bucketName, objectName);
    }

    public void renameFile(FileOperationDto fileOperationDto) {
        moveFile(fileOperationDto);
    }
}
