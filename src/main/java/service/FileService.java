package service;

import com.slava.entity.File;
import com.slava.repository.FileRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FileService {

    private final FileRepository fileRepository;

    public FileService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    // Сохранение файла
    public File saveFile(File file) {
        // Логика валидации
        if (file.getSize() <= 0) {
            throw new IllegalArgumentException("File size must be greater than 0");
        }
        return fileRepository.save(file);
    }

    // Получение всех файлов владельца
    public List<File> getFilesByOwner(Long ownerId) {
        return fileRepository.findByOwnerId(ownerId);
    }

    // Удаление файла по ID
    public void deleteFile(Long fileId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new EntityNotFoundException("File not found with ID: " + fileId));
        fileRepository.delete(file);
    }

    // Поиск файла по имени и пути
    public Optional<File> findByNameAndPath(String name, String path) {
        return Optional.ofNullable(fileRepository.findByNameAndPath(name, path));
    }
}

