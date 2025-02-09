package com.slava.controller;

import com.slava.dto.FileFolderDto;
import com.slava.dto.FileOperationDto;
import com.slava.service.FileService;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
            fileService.deleteFolder(fileOperationDto.getBucketName(), fileOperationDto.getSourcePath());
            redirectAttributes.addFlashAttribute("successMessage", "Folder deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting folder: " + e.getMessage());
        }
        return "redirect:/files/list?path=" + fileOperationDto.getSourcePath();
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFolder(@RequestParam("path") String path,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String bucketName = userDetails.getUsername();

            byte[] zipData = fileService.downloadFolderAsZip(bucketName, path);

            String folderName = extractFolderName(path);

            String zipFileName = folderName + ".zip";

            ByteArrayResource resource = new ByteArrayResource(zipData);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFileName + "\"")
                    .contentLength(zipData.length)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("Error while downloading folder", e);
        }
    }

    private String extractFolderName(String path) {
        if (path == null || path.isEmpty()) {
            return "Root"; // Если путь пустой, используем дефолтное имя
        }
        // Убираем конечный слеш, если он есть
        path = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;

        // Извлекаем имя папки из пути
        int lastSlashIndex = path.lastIndexOf("/");
        return (lastSlashIndex == -1) ? path : path.substring(lastSlashIndex + 1);
    }
}
