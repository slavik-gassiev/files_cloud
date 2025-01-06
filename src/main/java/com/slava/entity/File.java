package com.slava.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "files", uniqueConstraints = @UniqueConstraint(columnNames = {"path", "name"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "File name cannot be empty") // Проверка, что имя файла не пустое
    @Size(max = 255, message = "File name must not exceed 255 characters") // Ограничение длины имени
    private String name;

    @Column(nullable = false)
    @NotBlank(message = "File path cannot be empty") // Проверка, что путь не пустой
    @Size(max = 1024, message = "File path must not exceed 1024 characters") // Ограничение длины пути
    private String path;

    @Column(nullable = false)
    @NotNull(message = "File size cannot be null") // Размер файла должен быть указан
    @Min(value = 1, message = "File size must be greater than 0") // Проверка, что размер файла больше 0
    private Long size;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "File must have an owner") // Владелец файла должен быть указан
    private User owner;
}
