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
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/files")
public class FileController {

    private final FileService fileService;
    private final FolderService folderService;

    public FileController(FileService fileService, FolderService folderService) {
        this.fileService = fileService;
        this.folderService = folderService;
    }

    @GetMapping("/list")
    public String listFiles(@RequestParam(value = "path", required = false, defaultValue = "") String path,
                            @AuthenticationPrincipal UserDetails userDetails,
                            Model model) {

        String userName = userDetails.getUsername();

        String[] pathSegments = path.isEmpty() ? new String[0] : path.split("/");
        List<String> breadcrumbLinks = new ArrayList<>();
        StringBuilder fullPath = new StringBuilder();
        for (String segment : pathSegments) {
            if (!segment.isEmpty()) {
                fullPath.append(segment).append("/");
                breadcrumbLinks.add(fullPath.toString());
            }
        }
        List<FileFolderDto> folders = folderService.listOnlyFolders(userName);

        model.addAttribute("userName", userName);
        model.addAttribute("files", fileService.listFolderContents(userName, path));
        model.addAttribute("currentPath", path);
        model.addAttribute("pathSegments", pathSegments);
        model.addAttribute("breadcrumbLinks", breadcrumbLinks);
        model.addAttribute("folders", folders);
        return "files/list";
    }

    @PostMapping("/upload")
    public String uploadFile(
            @RequestParam("file") MultipartFile file,
            @ModelAttribute UploadFileDto uploadFileDto,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Uploaded file is empty");
            return "redirect:/files/list?path=" + uploadFileDto.getSourcePath();
        }

        uploadFileDto.setBucketName(userDetails.getUsername());
        uploadFileDto.setFileName(file.getOriginalFilename());
        uploadFileDto.setContentType(file.getContentType());
        try {
            uploadFileDto.setContent(file.getBytes());
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error reading file content");
            return "redirect:/files/list?path=" + uploadFileDto.getSourcePath();
        }

        if (uploadFileDto.getFileName() == null || uploadFileDto.getFileName().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "File name must not be empty");
            return "redirect:/files/list?path=" + uploadFileDto.getSourcePath();
        }

        if (uploadFileDto.getBucketName() == null || uploadFileDto.getBucketName().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bucket name must not be empty");
            return "redirect:/files/list?path=" + uploadFileDto.getSourcePath();
        }

        fileService.uploadFile(uploadFileDto);

        redirectAttributes.addFlashAttribute("successMessage", "File uploaded successfully");
        return "redirect:/files/list?path=" + uploadFileDto.getSourcePath();
    }

    @PostMapping("/move")
    public String moveFile(
            @ModelAttribute @Valid MoveFileDto moveFileDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка валидации: " + bindingResult.getAllErrors());
            return "redirect:/files/list?path=" + moveFileDto.getSourcePath();
        }

        moveFileDto.setBucketName(userDetails.getUsername());
        fileService.moveFile(moveFileDto);
        redirectAttributes.addFlashAttribute("successMessage", "Файл успешно перемещён");
        return "redirect:/files/list?path=" + moveFileDto.getTargetPath();
    }

    @PostMapping("/rename")
    public String renameFile(
            @ModelAttribute @Valid RenameFileDto renameFileDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation failed: " + bindingResult.getAllErrors());
            return "redirect:/files/list?path=" + renameFileDto.getSourcePath();
        }
        renameFileDto.setBucketName(userDetails.getUsername());
        try {
            fileService.renameFile(renameFileDto);
            redirectAttributes.addFlashAttribute("successMessage", "File renamed successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error renaming file: " + e.getMessage());
        }
        return "redirect:/files/list?path=" + fileService.getParentPathForFile(renameFileDto.getSourcePath());
    }

    @PostMapping("/delete")
    public String deleteFile(
            @ModelAttribute DeleteFileDto deleteFileDto,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            deleteFileDto.setBucketName(userDetails.getUsername());
            fileService.deleteFile(deleteFileDto);
            redirectAttributes.addFlashAttribute("successMessage", "File deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting file: " + e.getMessage());
        }
        return "redirect:/files/list?path=" + fileService.getParentPathForFile(deleteFileDto.getSourcePath());
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam("path") String path,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String bucketName = userDetails.getUsername();
            byte[] fileData = fileService.downloadFile(bucketName, path);
            String fileName = path.substring(path.lastIndexOf("/") + 1);

            ByteArrayResource resource = new ByteArrayResource(fileData);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentLength(fileData.length)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("Error while downloading file", e);
        }
    }
}
