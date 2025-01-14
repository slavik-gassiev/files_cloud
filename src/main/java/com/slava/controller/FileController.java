package com.slava.controller;

import com.slava.dto.FileDto;
import com.slava.service.FileService;
import com.slava.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/files")
public class FileController {

    private final FileService fileService;
    private final UserService userService;

    public FileController(FileService fileService, UserService userService) {
        this.fileService = fileService;
        this.userService = userService;
    }

    @ModelAttribute("file")
    public FileDto fileModelAttribute() {
        return new FileDto();
    }

    @GetMapping("/list")
    public String listFiles(Model model, @AuthenticationPrincipal Long userId) {
        model.addAttribute("files", fileService.getFilesByOwner(userId));
        return "files/list"; // Отображение списка файлов
    }

    @GetMapping("/upload")
    public String uploadPage() {
        return "files/upload";
    }

    @PostMapping("/upload")
    public String uploadFile(
            @Valid @ModelAttribute("file") FileDto fileDto,
            BindingResult bindingResult,
            @AuthenticationPrincipal Long userId,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "files/upload";
        }

        fileDto.setOwnerId(userId); // Устанавливаем текущего пользователя как владельца
        fileService.saveFile(fileDto); // Сохраняем файл через сервис

        redirectAttributes.addFlashAttribute("successMessage", "File uploaded successfully!");
        return "redirect:/files"; // Перенаправление на список файлов
    }

    @PostMapping("/delete/{id}")
    public String deleteFile(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        fileService.deleteFile(id); // Удаление файла
        redirectAttributes.addFlashAttribute("successMessage", "File deleted successfully!");
        return "redirect:/files";
    }
}
