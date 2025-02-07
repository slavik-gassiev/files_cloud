package com.slava.service;

import com.slava.dto.FileFolderDto;
import com.slava.dto.FileOperationDto;
import com.slava.repository.CustomFileRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
        String newTargetPath = buildNewPath(fileOperationDto.getSourcePath(), fileOperationDto.getTargetPath());
        fileRepository.moveFolder(fileOperationDto.getBucketName(), fileOperationDto.getSourcePath(), newTargetPath);
    }

    public void moveFolder(FileOperationDto fileOperationDto) {
        String folderName = extractFolderName(fileOperationDto.getSourcePath());
        String newTargetPath = normalizePath(fileOperationDto.getTargetPath()) + folderName + "/";

        fileRepository.moveFolder(fileOperationDto.getBucketName(), fileOperationDto.getSourcePath(), newTargetPath);
    }

    public void moveFile(FileOperationDto fileOperationDto) {
        String newTargetPath = fileOperationDto.getTargetPath();
        if (!newTargetPath.endsWith("/")) {
            newTargetPath += "/";
        }
        newTargetPath += extractFileName(fileOperationDto.getSourcePath());
        fileRepository.moveFile(fileOperationDto.getBucketName(), fileOperationDto.getSourcePath(), newTargetPath);
    }

    public void renameFile(FileOperationDto fileOperationDto) {
        String newTargetPath = buildNewPath(fileOperationDto.getSourcePath(), fileOperationDto.getTargetPath());
        fileRepository.moveFile(fileOperationDto.getBucketName(), fileOperationDto.getSourcePath(), newTargetPath);
    }

    public void deleteFile(FileOperationDto fileOperationDto) {
        fileRepository.deleteFile(fileOperationDto.getBucketName(), fileOperationDto.getSourcePath());
    }

    public byte[] downloadFolderAsZip(String bucketName, String folderPath) {
        List<String> files = fileRepository.listObjects(bucketName, folderPath);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (String filePath : files) {
                byte[] fileData = downloadFile(bucketName, filePath);
                ZipEntry entry = new ZipEntry(filePath.substring(folderPath.length()));
                zos.putNextEntry(entry);
                zos.write(fileData);
                zos.closeEntry();
            }
            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error while creating ZIP archive", e);
        }
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

    public void uploadFile(String bucketName, String objectName, byte[] content, String contentType) {
        fileRepository.uploadFile(bucketName, objectName, new ByteArrayInputStream(content), content.length, contentType);
    }

    public List<FileFolderDto> listOnlyFolders(String bucketName) {
        List<String> objects = fileRepository.listObjects(bucketName, "");

        return objects.stream()
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
    }

    public void ensureBucketExists(String bucketName) {
        if (!fileRepository.bucketExists(bucketName)) {
            fileRepository.createBucket(bucketName);
        }
    }

    private String buildNewPath(String sourcePath, String targetPath) {
        if (!sourcePath.endsWith("/")) {
            sourcePath += "/";
        }
        if (!targetPath.endsWith("/")) {
            targetPath += "/";
        }
        String folderName = extractFileName(sourcePath);
        return targetPath + folderName + "/";
    }

    private String extractFileName(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    private String extractFolderName(String sourcePath) {
        if (sourcePath.endsWith("/")) {
            sourcePath = sourcePath.substring(0, sourcePath.length() - 1);
        }
        return sourcePath.substring(sourcePath.lastIndexOf("/") + 1);
    }

    private String normalizePath(String path) {
        if (!path.endsWith("/")) {
            path += "/";
        }
        return path.replaceAll("//+", "/"); // Убираем двойные слэши
    }
}
