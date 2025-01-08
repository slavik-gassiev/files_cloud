package com.slava.controller;

import com.slava.entity.User;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.slava.service.UserService;

import java.io.File;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("user")
    public User userModelAttribute() {
        return new User();
    }

    @GetMapping("/login")
    public String loginPage() {
        System.out.println("Login page accessed");
        File file = new File("src/main/resources/templates/login.html");
        if (!file.exists()) {
            System.out.println("Файл login.html не найден!");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(
            @Valid @ModelAttribute("user") User user,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/register"; // Если ошибки валидации — вернуться на страницу
        }

        try {
            userService.registerUser(user); // Регистрация пользователя
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful!");
            return "redirect:/auth/login"; // Перенаправление на страницу входа
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("username", null, e.getMessage()); // Обработка ошибки уникальности имени пользователя
            return "auth/register";
        }
    }
}

