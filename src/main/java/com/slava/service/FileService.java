package com.slava.service;

import com.slava.dto.FileDto;
import com.slava.entity.File;
import com.slava.repository.FileRepository;
import com.slava.repository.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public FileService(FileRepository fileRepository, UserRepository userRepository, ModelMapper modelMapper) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
    }

    public List<FileDto> getFilesByOwner(Long ownerId) {
        return fileRepository.findByOwnerId(ownerId)
                .stream()
                .map(file -> modelMapper.map(file, FileDto.class))
                .collect(Collectors.toList());
    }

    public void saveFile(FileDto fileDto) {
        File file = modelMapper.map(fileDto, File.class);
        file.setOwner(userRepository.findById(fileDto.getOwnerId())
                .orElseThrow(() -> new IllegalArgumentException("Owner not found")));
        fileRepository.save(file);
    }

    public void deleteFile(Long fileId) {
        fileRepository.deleteById(fileId);
    }
}
