package com.slava.controller;

import com.slava.dto.FileOperationDto;
import com.slava.service.FileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/folders")
public class FolderController {

    private final FileService fileService;

    public FolderController(FileService fileService) {
        this.fileService = fileService;
    }

    @ModelAttribute("fileOperationDto")
    public FileOperationDto fileOperationDto(@AuthenticationPrincipal UserDetails userDetails) {
        FileOperationDto dto = new FileOperationDto();
        dto.setBucketName(userDetails.getUsername()); // Устанавливаем bucketName на основе имени пользователя
        return dto;
    }

    @PostMapping("/create")
    public String createFolder(
            @ModelAttribute @Valid FileOperationDto fileOperationDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation failed: " + bindingResult.getAllErrors());
            return "redirect:/files/list?path=" + fileOperationDto.getSourcePath();
        }

        try {
            fileService.createFolder(fileOperationDto.getBucketName(), fileOperationDto.getSourcePath() + fileOperationDto.getFolderName());
            redirectAttributes.addFlashAttribute("successMessage", "Folder created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating folder: " + e.getMessage());
        }
        return "redirect:/files/list?path=" + fileOperationDto.getSourcePath();
    }

    @PostMapping("/move")
    public String moveFolder(
            @ModelAttribute @Valid FileOperationDto fileOperationDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка валидации: " + bindingResult.getAllErrors());
            return "redirect:/files/list?path=" + fileOperationDto.getSourcePath();
        }

        try {
            fileService.moveFolder(fileOperationDto);
            redirectAttributes.addFlashAttribute("successMessage", "Папка успешно перемещена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при перемещении папки: " + e.getMessage());
        }
        return "redirect:/files/list?path=" + fileOperationDto.getTargetPath();
    }

    @PostMapping("/rename")
    public String renameFolder(
            @ModelAttribute @Valid FileOperationDto fileOperationDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation failed: " + bindingResult.getAllErrors());
            return "redirect:/files/list?path=" + fileOperationDto.getSourcePath();
        }

        try {
            fileService.renameFolder(fileOperationDto);
            redirectAttributes.addFlashAttribute("successMessage", "Folder renamed successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error renaming folder: " + e.getMessage());
        }
        return "redirect:/files/list?path=" + fileOperationDto.getSourcePath();
    }

    @PostMapping("/delete")
    public String deleteFolder(
            @ModelAttribute FileOperationDto fileOperationDto,
            RedirectAttributes redirectAttributes) {
        try {
            fileService.deleteFolder(fileOperationDto.getBucketName(), fileOperationDto.getSourcePath() + fileOperationDto.getFolderName());
            redirectAttributes.addFlashAttribute("successMessage", "Folder deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting folder: " + e.getMessage());
        }
        return "redirect:/files/list?path=" + fileOperationDto.getSourcePath();
    }
}
