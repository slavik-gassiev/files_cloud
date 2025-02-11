package com.slava.service;

import com.slava.dto.*;
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

    public void createFolder(CreateFolderDto createFolderDto) {
        createFolderDto.setSourcePath(createFolderDto.getSourcePath() + createFolderDto.getFolderName());
        fileRepository.createFolder(createFolderDto.getBucketName(), createFolderDto.getSourcePath());
    }

    public void deleteFolder(DeleteFileDto deleteFileDto) {
        fileRepository.deleteFolder(deleteFileDto.getBucketName(), deleteFileDto.getSourcePath());
    }

    public void renameFolder(RenameFileDto fileOperationDto) {
        String newTargetPath = fileOperationDto.getSourcePath().replace(fileOperationDto.getFileName(), fileOperationDto.getNewFileName());
        fileRepository.moveFolder(fileOperationDto.getBucketName(), fileOperationDto.getSourcePath(), newTargetPath);
    }

    public void moveFolder(MoveFileDto fileOperationDto) {
        String newTargetPath = fileOperationDto.getTargetPath() + fileOperationDto.getFileName();
        fileRepository.moveFolder(fileOperationDto.getBucketName(), fileOperationDto.getSourcePath(), newTargetPath);
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
}
