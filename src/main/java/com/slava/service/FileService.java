package com.slava.service;

import com.slava.dto.FileFolderDto;
import com.slava.dto.FileOperationDto;
import com.slava.repository.CustomFileRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@Service
public class FileService {

    private final CustomFileRepository fileRepository;
    private final ModelMapper modelMapper;

    public FileService(CustomFileRepository fileRepository, ModelMapper modelMapper) {
        this.fileRepository = fileRepository;
        this.modelMapper = modelMapper;
    }

    // Создание папки
    public void createFolder(String bucketName, String path) {
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        fileRepository.uploadFile(bucketName, path, new ByteArrayInputStream(new byte[0]), 0, "application/x-directory");
    }

    // Удаление папки
    public void deleteFolder(String bucketName, String path) {
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        List<String> objects = fileRepository.listObjects(bucketName, path);
        for (String object : objects) {
            fileRepository.deleteFile(bucketName, object);
        }
    }

    // Переименование папки
    public void renameFolder(FileOperationDto fileOperationDto) {
        moveFolder(fileOperationDto);
    }

    // Перемещение папки
    public void moveFolder(FileOperationDto fileOperationDto) {
        String bucketName = fileOperationDto.getBucketName();
        String sourcePath = fileOperationDto.getSourcePath();
        String targetPath = fileOperationDto.getTargetPath();

        if (!sourcePath.endsWith("/")) {
            sourcePath = sourcePath + "/";
        }
        if (!targetPath.endsWith("/")) {
            targetPath = targetPath + "/";
        }

        List<String> objects = fileRepository.listObjects(bucketName, sourcePath);
        for (String object : objects) {
            String relativePath = object.substring(sourcePath.length());
            String newObjectName = targetPath + relativePath;
            fileRepository.copyFile(bucketName, object, newObjectName);
            fileRepository.deleteFile(bucketName, object);
        }
    }

    // Перемещение файла
    public void moveFile(FileOperationDto fileOperationDto) {
        String bucketName =  fileOperationDto.getBucketName();
        fileRepository.copyFile(bucketName, fileOperationDto.getSourcePath(), fileOperationDto.getTargetPath());
        fileRepository.deleteFile(bucketName, fileOperationDto.getSourcePath());
    }

    // Список содержимого папки
    public List<FileFolderDto> listFolderContents(String bucketName, String path) {
        String folderPath = (path == null || path.isEmpty()) ? "" : (path.endsWith("/") ? path : path + "/");
        List<String> objects = fileRepository.listObjects(bucketName, folderPath);

        return objects.stream()
                .filter(object -> {
                    String relativePath = object.substring(folderPath.length());
                    // Учитываем только элементы на текущем уровне
                    return !relativePath.isEmpty() && (relativePath.indexOf('/') == relativePath.length() - 1);
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




    // Загрузка файла
    public void uploadFile(String bucketName, String objectName, byte[] content, String contentType) {
        fileRepository.uploadFile(bucketName, objectName, new ByteArrayInputStream(content), content.length, contentType);
    }

    // Загрузка содержимого файла
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

    public void ensureBucketExists(String bucketName) {
        try {
            boolean exists = fileRepository.bucketExists(bucketName);
            if (!exists) {
                fileRepository.createBucket(bucketName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error ensuring bucket exists: " + bucketName, e);
        }
    }

    // Удаление файла
    public void deleteFile(String bucketName, String objectName) {
        fileRepository.deleteFile(bucketName, objectName);
    }

    // Переименование файла
    public void renameFile(FileOperationDto fileOperationDto) {
        moveFile(fileOperationDto);
    }
}
