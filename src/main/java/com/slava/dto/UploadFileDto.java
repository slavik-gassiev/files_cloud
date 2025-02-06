package com.slava.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class UploadFileDto {
    @NotNull
    private String bucketName;

    @NotNull
    private String sourcePath;

    @NotNull
    @Size(min = 1)
    private String fileName;

    private byte[] content;

    private String contentType;
}

