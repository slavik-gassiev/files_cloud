package com.slava.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class RenameFileDto {
    @NotNull
    private String bucketName;

    @NotNull
    private String sourcePath;

    @NotNull
    private String fileName;

    @NotNull
    private String newFileName;

    private boolean isFolder = false;
}

