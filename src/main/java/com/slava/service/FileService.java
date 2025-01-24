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
    public void createFolder(String path) {
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        fileRepository.uploadFile(path, new ByteArrayInputStream(new byte[0]), 0, "application/x-directory");
    }

    // Удаление папки
    public void deleteFolder(String path) {
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        List<String> objects = fileRepository.listObjects(path);
        for (String object : objects) {
            fileRepository.deleteFile(object);
        }
    }

    // Переименование папки
    public void renameFolder(FileOperationDto fileOperationDto) {
        moveFolder(fileOperationDto);
    }

    // Перемещение папки
    public void moveFolder(FileOperationDto fileOperationDto) {
        String sourcePath = fileOperationDto.getSourcePath();
        String targetPath = fileOperationDto.getTargetPath();

        if (!sourcePath.endsWith("/")) {
            sourcePath = sourcePath + "/";
        }
        if (!targetPath.endsWith("/")) {
            targetPath = targetPath + "/";
        }

        List<String> objects = fileRepository.listObjects(sourcePath);
        for (String object : objects) {
            String relativePath = object.substring(sourcePath.length());
            String newObjectName = targetPath + relativePath;
            fileRepository.copyFile(object, newObjectName);
            fileRepository.deleteFile(object);
        }
    }

    // Перемещение файла
    public void moveFile(FileOperationDto fileOperationDto) {
        fileRepository.copyFile(fileOperationDto.getSourcePath(), fileOperationDto.getTargetPath());
        fileRepository.deleteFile(fileOperationDto.getSourcePath());
    }

    // Список содержимого папки
    public List<FileFolderDto> listFolderContents(String path) {
//        String folderPath = path.endsWith("/") ? path : path + "/"; // Используем локальную переменную
        List<String> objects = fileRepository.listObjects(path);
        return objects.stream()
                .map(object -> {
                    FileFolderDto dto = new FileFolderDto();
                    dto.setName(object.substring(path.length()));
                    dto.setPath(object);
                    dto.setFolder(object.endsWith("/"));
                    dto.setSize(0);
                    return dto;
                })
                .toList();
    }


    // Загрузка файла
    public void uploadFile(String objectName, byte[] content, String contentType) {
        fileRepository.uploadFile(objectName, new ByteArrayInputStream(content), content.length, contentType);
    }

    // Загрузка содержимого файла
    public byte[] downloadFile(String objectName) {
        return fileRepository.downloadFile(objectName)
                .map(inputStream -> {
                    try {
                        return inputStream.readAllBytes();
                    } catch (IOException e) {
                        throw new RuntimeException("Error reading file content", e);
                    }
                })
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
    }


    // Удаление файла
    public void deleteFile(String objectName) {
        fileRepository.deleteFile(objectName);
    }

    // Переименование файла
    public void renameFile(FileOperationDto fileOperationDto) {
        moveFile(fileOperationDto);
    }
}
