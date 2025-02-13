package com.slava.service;

import com.slava.dto.*;
import com.slava.exception.FolderDownloadException;
import com.slava.repository.CustomFileRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FolderService {
    private final CustomFileRepository fileRepository;
    private final FileService fileService;

    public FolderService(CustomFileRepository fileRepository, FileService fileService) {
        this.fileRepository = fileRepository;
        this.fileService = fileService;
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

    public byte[] downloadFolderAsZip(String bucketName, String folderPath) {
        List<String> files = fileRepository.listObjects(bucketName, folderPath);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (String filePath : files) {
                byte[] fileData = fileService.downloadFile(bucketName, filePath);
                ZipEntry entry = new ZipEntry(filePath.substring(folderPath.length()));
                zos.putNextEntry(entry);
                zos.write(fileData);
                zos.closeEntry();
            }
            zos.finish();

            return baos.toByteArray();
        } catch (IOException e) {
            throw new FolderDownloadException("Error while creating ZIP archive");
        }
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

    public String getParentPathForFolder(String fullPath) {
        if (fullPath.endsWith("/")) {
            fullPath = fullPath.substring(0, fullPath.length() - 1);
        }
        int lastSlashIndex = fullPath.lastIndexOf("/");
        return (lastSlashIndex != -1) ? fullPath.substring(0, lastSlashIndex) + "/" : "";
    }

    public String extractFolderName(String path) {
        if (path == null || path.isEmpty()) {
            return "Root";
        }
        path = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;

        int lastSlashIndex = path.lastIndexOf("/");
        return (lastSlashIndex == -1) ? path : path.substring(lastSlashIndex + 1);
    }
}
