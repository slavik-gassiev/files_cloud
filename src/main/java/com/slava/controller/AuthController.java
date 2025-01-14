package com.slava.controller;

import com.slava.dto.UserDto;
import com.slava.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("user")
    public UserDto userModelAttribute() {
        return new UserDto();
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }


    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(
            @Valid @ModelAttribute("user") UserDto userDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "auth/register"; // Если ошибки валидации — вернуться на страницу регистрации
        }

        try {
            userService.registerUser(userDto); // Регистрация пользователя через DTO
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful!");
            return "files/list"; // Перенаправление на страницу входа
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("username", null, e.getMessage()); // Обработка ошибки уникальности имени пользователя
            return "auth/register";
        }
    }
}
