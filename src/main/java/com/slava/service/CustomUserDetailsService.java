package com.slava.service;

import com.slava.config.CustomUserDetails;
import com.slava.dto.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);
    private final UserService userService;

    public CustomUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Попытка загрузки пользователя по логину: {}", username);
        try {
            UserDto userDto = userService.getUserWithName(username);
            log.debug("Пользователь '{}' успешно найден", username);
            return new CustomUserDetails(userDto);
        } catch (Exception e) {
            log.error("Ошибка при загрузке пользователя по логину '{}': {}", username, e.getMessage());
            throw new UsernameNotFoundException("Пользователь не найден");
        }
    }
}
