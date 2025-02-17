package com.slava.service;

import com.slava.config.MinioConfig;
import com.slava.dto.UserDto;
import com.slava.entity.Role;
import com.slava.entity.User;
import com.slava.exception.UserAlreadyExists;
import com.slava.exception.UserException;
import com.slava.repository.UserRepository;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private RoleService roleService;

    @Mock
    private MinioConfig minioConfig;

    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private UserService userService;

    @Test
    void findByUserName_ShouldReturnUser_WhenUserExists() {
        User user = new User();
        user.setUsername("userTest");
        when(userRepository.findByUsername("userTest")).thenReturn(Optional.of(user));

        Optional<User> foundUser = userService.findByUsername("userTest");

        System.out.println("Mocked repo response: " + foundUser);
        assertTrue(foundUser.isPresent());
        assertEquals("userTest", foundUser.get().getUsername());
    }

    @Test
    void registerUser_Success() {
        UserDto userDto = new UserDto();
        userDto.setUsername("newUser");
        userDto.setPassword("pass");

        when(userRepository.findByUsername("newUser")).thenReturn(Optional.empty());

        User mappedUser = new User();
        mappedUser.setUsername("newUser");
        mappedUser.setRoles(new HashSet<>());
        mappedUser.setPassword("pass");
        when(modelMapper.map(userDto, User.class)).thenReturn(mappedUser);

        when(passwordEncoder.encode("pass")).thenReturn("encodedPass");

        Role defaultRole = new Role();
        defaultRole.setName("ROLE_USER");
        when(roleService.findByName("ROLE_USER")).thenReturn(Optional.of(defaultRole));

        when(userRepository.save(mappedUser)).thenReturn(mappedUser);

        User registeredUser = userService.registerUser(userDto);

        verify(minioConfig).initializeUserRootFolder(minioClient, "newUser");
        verify(userRepository).save(mappedUser);

        assertEquals("newUser", registeredUser.getUsername());
        assertEquals("encodedPass", registeredUser.getPassword());
        assertTrue(registeredUser.getRoles().contains(defaultRole));
    }

    @Test
    void registerUser_UserAlreadyExists_ThrowsException() {
        UserDto userDto = new UserDto();
        userDto.setUsername("existingUser");
        userDto.setPassword("pass");

        when(userRepository.findByUsername("existingUser")).thenReturn(Optional.of(new User()));

        assertThrows(UserAlreadyExists.class, () -> userService.registerUser(userDto));
    }

    @Test
    void setDefaultRole_AddsDefaultRoleToUser() {
        User user = new User();
        user.setUsername("testUser");
        user.setRoles(new HashSet<>());

        Role defaultRole = new Role();
        defaultRole.setName("ROLE_USER");
        when(roleService.findByName("ROLE_USER")).thenReturn(Optional.of(defaultRole));

        userService.setDefaultRole(user);

        assertTrue(user.getRoles().contains(defaultRole));
    }

    @Test
    void getUserWithName_ReturnsUserDto_WhenUserExists() {
        User user = new User();
        user.setUsername("userTest");
        user.setRoles(new HashSet<>());

        UserDto expectedDto = new UserDto();
        expectedDto.setUsername("userTest");

        when(userRepository.findByUsernameWithRoles("userTest")).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserDto.class)).thenReturn(expectedDto);

        UserDto result = userService.getUserWithName("userTest");
        assertEquals("userTest", result.getUsername());
    }

    @Test
    void assignRoleToUser_AddsRoleAndSavesUser() {
        User user = new User();
        user.setUsername("testUser");
        user.setRoles(new HashSet<>());

        Role role = new Role();
        role.setName("ROLE_ADMIN");

        userService.assignRoleToUser(user, role);

        assertTrue(user.getRoles().contains(role));
        verify(userRepository).save(user);
    }

    @Test
    void getAllUsers_ReturnsListOfUserDtos() {
        User user1 = new User();
        user1.setUsername("user1");
        user1.setRoles(new HashSet<>());
        User user2 = new User();
        user2.setUsername("user2");
        user2.setRoles(new HashSet<>());

        List<User> userList = Arrays.asList(user1, user2);
        when(userRepository.findAll()).thenReturn(userList);

        UserDto userDto1 = new UserDto();
        userDto1.setUsername("user1");
        UserDto userDto2 = new UserDto();
        userDto2.setUsername("user2");

        when(modelMapper.map(user1, UserDto.class)).thenReturn(userDto1);
        when(modelMapper.map(user2, UserDto.class)).thenReturn(userDto2);

        List<UserDto> result = userService.getAllUsers();
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dto -> "user1".equals(dto.getUsername())));
        assertTrue(result.stream().anyMatch(dto -> "user2".equals(dto.getUsername())));
    }

    @Test
    void findById_ReturnsUser_WhenUserExists() {
        User user = new User();
        user.setUsername("testUser");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findById(1L);
        assertTrue(result.isPresent());
        assertEquals("testUser", result.get().getUsername());
    }

    @Test
    void deleteUser_CallsRepositoryDeleteById() {
        Long userId = 1L;
        userService.deleteUser(userId);
        verify(userRepository).deleteById(userId);
    }
}
