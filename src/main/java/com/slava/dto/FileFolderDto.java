package com.slava.dto;

import lombok.Data;

@Data
public class FileFolderDto {
    private String name;
    private String path;
    private boolean isFolder;
    private long size;
    private String contentType;
}
