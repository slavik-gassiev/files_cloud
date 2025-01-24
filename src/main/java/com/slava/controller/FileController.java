package com.slava.controller;

import com.slava.dto.FileOperationDto;
import com.slava.service.FileService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @ModelAttribute("fileOperationDto")
    public FileOperationDto fileOperationDto() {
        return new FileOperationDto();
    }

    @GetMapping("/list")
    public String listFiles(@RequestParam(value = "path", required = false, defaultValue = "") String path, Model model) {
        model.addAttribute("files", fileService.listFolderContents(path));
        model.addAttribute("currentPath", path);
        return "files/list";
    }

    @PostMapping("/upload")
    public String uploadFile(
            @RequestParam("file") MultipartFile file, // Используем MultipartFile для загрузки файла
            @ModelAttribute @Valid FileOperationDto fileOperationDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation failed: " + bindingResult.getAllErrors());
            return "redirect:/files/list?path=" + fileOperationDto.getSourcePath();
        }

        try {
            // Проверяем, что файл не пустой
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Uploaded file is empty");
            }

            // Передаем содержимое файла и другие данные в сервис
            fileService.uploadFile(
                    fileOperationDto.getSourcePath() + file.getOriginalFilename(),
                    file.getBytes(),
                    file.getContentType()
            );
            redirectAttributes.addFlashAttribute("successMessage", "File uploaded successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error uploading file: " + e.getMessage());
        }
        return "redirect:/files/list?path=" + fileOperationDto.getSourcePath();
    }


    @PostMapping("/rename")
    public String renameFile(
            @ModelAttribute @Valid FileOperationDto fileOperationDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation failed: " + bindingResult.getAllErrors());
            return "redirect:/files/list?path=" + fileOperationDto.getSourcePath();
        }

        try {
            fileService.renameFile(fileOperationDto);
            redirectAttributes.addFlashAttribute("successMessage", "File renamed successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error renaming file: " + e.getMessage());
        }
        return "redirect:/files/list?path=" + fileOperationDto.getSourcePath();
    }

    @PostMapping("/delete")
    public String deleteFile(
            @ModelAttribute FileOperationDto fileOperationDto,
            RedirectAttributes redirectAttributes) {
        try {
            fileService.deleteFile(fileOperationDto.getSourcePath() + fileOperationDto.getFileName());
            redirectAttributes.addFlashAttribute("successMessage", "File deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting file: " + e.getMessage());
        }
        return "redirect:/files/list?path=" + fileOperationDto.getSourcePath();
    }
}
