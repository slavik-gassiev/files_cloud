package com.slava.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class CreateFolderDto {
    @NotNull
    private String bucketName;

    @NotNull
    private String sourcePath;

    @NotNull
    private String folderName;
}

