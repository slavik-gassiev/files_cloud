package com.slava.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class MoveFileDto {
    @NotNull
    private String bucketName;

    @NotNull
    private String sourcePath;

    @NotNull
    private String targetPath;

    @NotNull
    private String fileName;
    private boolean isFolder = false;
}

