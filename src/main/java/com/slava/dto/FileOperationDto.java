package com.slava.dto;

import lombok.Data;

@Data
public class FileOperationDto {
    private String sourcePath;
    private String targetPath;
    private String fileName;
    private String folderName;
}
