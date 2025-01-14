package com.slava.config;

import com.slava.config.CustomUserDetails;
import com.slava.dto.UserDto;
import com.slava.repository.UserRepository;
import com.slava.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService;

    public CustomUserDetailsService(UserRepository userRepository, UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDto userDto = userService.getUserWithRoles(username);
        return new CustomUserDetails(userDto);
    }
}
