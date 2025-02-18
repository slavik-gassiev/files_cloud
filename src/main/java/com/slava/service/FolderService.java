package com.slava.service;

import com.slava.dto.*;
import com.slava.exception.FolderDownloadException;
import com.slava.repository.CustomFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FolderService {
    private static final Logger log = LoggerFactory.getLogger(FolderService.class);
    private final CustomFileRepository fileRepository;
    private final FileService fileService;

    public FolderService(CustomFileRepository fileRepository, FileService fileService) {
        this.fileRepository = fileRepository;
        this.fileService = fileService;
    }

    public void createFolder(CreateFolderDto createFolderDto) {
        if (!createFolderDto.getSourcePath().endsWith("/") && !createFolderDto.getSourcePath().isBlank()) {
            createFolderDto.setSourcePath(createFolderDto.getSourcePath() + "/");
        }
        String finalPath = createFolderDto.getSourcePath() + createFolderDto.getFolderName();
        log.info("Создание папки в бакете '{}': путь '{}'", createFolderDto.getBucketName(), finalPath);
        createFolderDto.setSourcePath(finalPath);
        fileRepository.createFolder(createFolderDto.getBucketName(), createFolderDto.getSourcePath());
    }

    public void deleteFolder(DeleteFileDto deleteFileDto) {
        log.info("Удаление папки в бакете '{}', путь '{}'", deleteFileDto.getBucketName(), deleteFileDto.getSourcePath());
        fileRepository.deleteFolder(deleteFileDto.getBucketName(), deleteFileDto.getSourcePath());
    }

    public void renameFolder(RenameFileDto fileOperationDto) {
        String newTargetPath = fileOperationDto.getSourcePath().replace(fileOperationDto.getFileName(), fileOperationDto.getNewFileName());
        log.info("Переименование папки в бакете '{}': с '{}' на '{}'", fileOperationDto.getBucketName(), fileOperationDto.getFileName(), fileOperationDto.getNewFileName());
        fileRepository.moveFolder(fileOperationDto.getBucketName(), fileOperationDto.getSourcePath(), newTargetPath);
    }

    public void moveFolder(MoveFileDto fileOperationDto) {
        String newTargetPath = fileOperationDto.getTargetPath() + fileOperationDto.getFileName();
        log.info("Перемещение папки в бакете '{}': исходный путь '{}', новый путь '{}'", fileOperationDto.getBucketName(), fileOperationDto.getSourcePath(), newTargetPath);
        fileRepository.moveFolder(fileOperationDto.getBucketName(), fileOperationDto.getSourcePath(), newTargetPath);
    }

    public byte[] downloadFolderAsZip(String bucketName, String folderPath) {
        log.info("Создание ZIP-архива для папки '{}' в бакете '{}'", folderPath, bucketName);
        List<String> files = fileRepository.listObjects(bucketName, folderPath);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (String filePath : files) {
                log.debug("Добавление файла '{}' в ZIP", filePath);
                byte[] fileData = fileService.downloadFile(bucketName, filePath);
                ZipEntry entry = new ZipEntry(filePath.substring(folderPath.length()));
                zos.putNextEntry(entry);
                zos.write(fileData);
                zos.closeEntry();
            }
            zos.finish();
            log.info("ZIP-архив для папки '{}' успешно создан ({} байт)", folderPath, baos.size());
            return baos.toByteArray();
        } catch (IOException | RuntimeException ex) {
            log.error("Ошибка при создании ZIP-архива для папки '{}': {}", folderPath, ex.getMessage(), ex);
            throw new FolderDownloadException("Ошибка при создании ZIP-архива");
        }
    }

    public List<FileFolderDto> listOnlyFolders(String bucketName) {
        log.debug("Получение списка папок из бакета '{}'", bucketName);
        List<String> objects = fileRepository.listObjects(bucketName, "");
        List<FileFolderDto> folders = objects.stream()
                .filter(object -> object.endsWith("/"))
                .map(object -> {
                    FileFolderDto dto = new FileFolderDto();
                    dto.setName(object);
                    dto.setPath(object);
                    dto.setFolder(true);
                    dto.setSize(0);
                    return dto;
                })
                .toList();
        log.debug("Найдено {} папок в бакете '{}'", folders.size(), bucketName);
        return folders;
    }

    public String getParentPathForFolder(String fullPath) {
        log.debug("Определение родительского пути для '{}'", fullPath);
        if (fullPath.endsWith("/")) {
            fullPath = fullPath.substring(0, fullPath.length() - 1);
        }
        int lastSlashIndex = fullPath.lastIndexOf("/");
        String parentPath = (lastSlashIndex != -1) ? fullPath.substring(0, lastSlashIndex) + "/" : "";
        log.debug("Родительский путь: '{}'", parentPath);
        return parentPath;
    }

    public String extractFolderName(String path) {
        log.debug("Извлечение имени папки из пути '{}'", path);
        if (path == null || path.isEmpty()) {
            log.debug("Путь пустой или null, возвращаем 'Root'");
            return "Root";
        }
        path = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int lastSlashIndex = path.lastIndexOf("/");
        String folderName = (lastSlashIndex == -1) ? path : path.substring(lastSlashIndex + 1);
        log.debug("Извлечённое имя папки: '{}'", folderName);
        return folderName;
    }
}
