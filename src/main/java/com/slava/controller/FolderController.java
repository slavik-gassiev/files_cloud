package com.slava.controller;

import com.slava.dto.*;
import com.slava.service.FileService;
import com.slava.service.FolderService;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/folders")
public class FolderController {

    private final FolderService folderService;
    private final FileService fileService;

    public FolderController(FolderService folderService, FileService fileService) {
        this.folderService = folderService;
        this.fileService = fileService;
    }

    @PostMapping("/create")
    public String createFolder(
            @ModelAttribute @Valid CreateFolderDto createFolderDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation failed: " + bindingResult.getAllErrors());
            return "redirect:/files/list?path=" + createFolderDto.getSourcePath();
        }

        try {
            createFolderDto.setBucketName(userDetails.getUsername());
            folderService.createFolder(createFolderDto);
            redirectAttributes.addFlashAttribute("successMessage", "Folder created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating folder: " + e.getMessage());
        }
        return "redirect:/files/list?path=" + createFolderDto.getSourcePath();
    }

    @PostMapping("/move")
    public String moveFolder(
            @ModelAttribute @Valid MoveFileDto moveFileDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка валидации: " + bindingResult.getAllErrors());
            return "redirect:/files/list?path=" + moveFileDto.getSourcePath();
        }

        try {
            moveFileDto.setBucketName(userDetails.getUsername());
            folderService.moveFolder(moveFileDto);
            redirectAttributes.addFlashAttribute("successMessage", "Папка успешно перемещена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при перемещении папки: " + e.getMessage());
        }
        return "redirect:/files/list?path=" + moveFileDto.getTargetPath();
    }

    @PostMapping("/rename")
    public String renameFolder(
            @ModelAttribute @Valid RenameFileDto renameFileDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation failed: " + bindingResult.getAllErrors());
            return "redirect:/files/list?path=" + renameFileDto.getSourcePath();
        }

        try {
            renameFileDto.setBucketName(userDetails.getUsername());
            folderService.renameFolder(renameFileDto);
            redirectAttributes.addFlashAttribute("successMessage", "Folder renamed successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error renaming folder: " + e.getMessage());
        }
        return "redirect:/files/list?path=" + folderService.getParentPathForFolder(renameFileDto.getSourcePath());
    }

    @PostMapping("/delete")
    public String deleteFolder(
            @ModelAttribute DeleteFileDto deleteFileDto,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            deleteFileDto.setBucketName(userDetails.getUsername());
            folderService.deleteFolder(deleteFileDto);
            redirectAttributes.addFlashAttribute("successMessage", "Folder deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting folder: " + e.getMessage());
        }
        return "redirect:/files/list?path=" + folderService.getParentPathForFolder(deleteFileDto.getSourcePath());
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFolder(@RequestParam("path") String path,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String bucketName = userDetails.getUsername();

            byte[] zipData = folderService.downloadFolderAsZip(bucketName, path);

            String folderName = folderService.extractFolderName(path);

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
}
