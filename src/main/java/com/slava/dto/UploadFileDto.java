package com.slava.dto;

import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

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

    @Transient
    private MultipartFile file;
}

