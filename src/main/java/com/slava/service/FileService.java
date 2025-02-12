package com.slava.service;

import com.slava.dto.*;
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

    public void moveFile(MoveFileDto fileOperationDto) {
        String newTargetPath = fileOperationDto.getTargetPath() + fileOperationDto.getFileName();
        fileRepository.moveFile(fileOperationDto.getBucketName(), fileOperationDto.getSourcePath(), newTargetPath);
    }

    public void renameFile(RenameFileDto fileOperationDto) {
        String newTargetPath = fileOperationDto.getSourcePath().replace(fileOperationDto.getFileName(), fileOperationDto.getNewFileName());
        fileRepository.moveFile(fileOperationDto.getBucketName(), fileOperationDto.getSourcePath(), newTargetPath);
    }

    public void deleteFile(DeleteFileDto deleteFileDto) {
        fileRepository.deleteFile(deleteFileDto.getBucketName(), deleteFileDto.getSourcePath());
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

    public List<FileFolderDto> listFolderContents(String bucketName, String path) {
        String folderPath = (path == null || path.isEmpty()) ? "" : (path.endsWith("/") ? path : path + "/");
        List<String> objects = fileRepository.listObjects(bucketName, folderPath);

        return objects.stream()
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
                    dto.setSize(0); // MinIO не возвращает размер для папок, оставляем 0
                    return dto;
                })
                .toList();
    }

    public void uploadFile(UploadFileDto uploadFileDto) {
        String objectName = uploadFileDto.getSourcePath() + uploadFileDto.getFileName();

        fileRepository.uploadFile(
                uploadFileDto.getBucketName(),
                objectName,
                new ByteArrayInputStream(uploadFileDto.getContent()),
                uploadFileDto.getContent().length,
                uploadFileDto.getContentType()
        );
    }

    public void ensureBucketExists(String bucketName) {
        if (!fileRepository.bucketExists(bucketName)) {
            fileRepository.createBucket(bucketName);
        }
    }

    public String getParentPathForFile(String fullPath) {
        int lastSlashIndex = fullPath.lastIndexOf("/");
        return (lastSlashIndex != -1) ? fullPath.substring(0, lastSlashIndex) : "";
    }
}
