package com.slava.controller;

import com.slava.entity.File;
import com.slava.entity.User;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.slava.service.FileService;

@Controller
@RequestMapping("/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @ModelAttribute("file")
    public File fileModelAttribute() {
        return new File();
    }

    @GetMapping
    public String listFiles(Model model, @AuthenticationPrincipal User user) {
        model.addAttribute("files", fileService.getFilesByOwner(user.getId()));
        return "files/list"; // Отображение списка файлов
    }

    @GetMapping("/upload")
    public String uploadPage() {
        return "files/upload";
    }

    @PostMapping("/upload")
    public String uploadFile(
            @Valid @ModelAttribute("file") File file,
            BindingResult bindingResult,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "files/upload";
        }

        file.setOwner(user);
        fileService.saveFile(file);

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

