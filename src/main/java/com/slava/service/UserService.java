package com.slava.service;

import com.slava.config.MinioConfig;
import com.slava.dto.RoleDto;
import com.slava.dto.UserDto;
import com.slava.entity.Role;
import com.slava.entity.User;
import com.slava.exception.UserAlreadyExists;
import com.slava.exception.UserException;
import com.slava.repository.UserRepository;
import io.minio.MinioClient;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final RoleService roleService;
    private final MinioConfig minioConfig;
    private final MinioClient minioClient;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, ModelMapper modelMapper, RoleService roleService, MinioConfig minioConfig, MinioClient minioClient) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
        this.roleService = roleService;
        this.minioConfig = minioConfig;
        this.minioClient = minioClient;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User registerUser(UserDto userDto) {
        findByUsername(userDto.getUsername()).ifPresent(u -> {
            System.out.println("User already exists");
            throw new UserAlreadyExists("User already exists");
        });

        User user = modelMapper.map(userDto, User.class);
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        setDefaultRole(user);
        minioConfig.initializeUserRootFolder(minioClient, user.getUsername());
        return userRepository.save(user);
    }

    public void assignRoleToUser(User user, Role role) {
        user.getRoles().add(role);
        userRepository.save(user);
    }

    public void setDefaultRole(User user) {
        Role userRole = roleService.findByName("ROLE_USER")
                .orElseThrow(() -> new UserException("Default role not found"));
        user.getRoles().add(userRole);
    }

    public UserDto getUserWithName(String username) {
        User user = userRepository.findByUsernameWithRoles(username)
                .orElseThrow(() -> new UserException("User not found"));
        return modelMapper.map(user, UserDto.class);
    }

    public UserDto mapToUserDto(User user) {
        UserDto userDto = modelMapper.map(user, UserDto.class);
        userDto.setRoles(user.getRoles().stream()
                .map(role -> modelMapper.map(role, RoleDto.class))
                .toList());
        return userDto;
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserDto)
                .toList();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
