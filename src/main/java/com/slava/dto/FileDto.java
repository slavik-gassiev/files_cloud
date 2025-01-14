package com.slava.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FileDto {

    private Long id;

    @NotBlank(message = "File name cannot be empty")
    @Size(max = 255, message = "File name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "File path cannot be empty")
    @Size(max = 1024, message = "File path must not exceed 1024 characters")
    private String path;

    @NotNull(message = "File size cannot be null")
    @Min(value = 1, message = "File size must be greater than 0")
    private Long size;

    @NotNull(message = "File must have an owner")
    private Long ownerId;
}
