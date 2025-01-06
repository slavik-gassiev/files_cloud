package com.slava.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // Отключаем CSRF (если необходимо)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/register", "/login").permitAll() // Доступ для всех
                        .anyRequest().authenticated() // Остальные запросы требуют авторизации
                )
                .formLogin(form -> form
                        .defaultSuccessUrl("/home", true) // URL после успешного входа
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login") // URL после выхода
                );

        return http.build();
    }
}

