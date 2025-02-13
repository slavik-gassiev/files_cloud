package com.slava.exception;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FileNotFoundException.class)
    public String handleFileNotFound(FileNotFoundException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "File/Folder not found: " + ex.getMessage());
        return "redirect:/files/list";
    }

    @ExceptionHandler(FileAlreadyExistsException.class)
    public String handleFileAlreadyExists(FileAlreadyExistsException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "File/Folder already exists: " + ex.getMessage());
        return "redirect:/files/list";
    }

    @ExceptionHandler(InvalidFileNameException.class)
    public String handleInvalidFileName(InvalidFileNameException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "Invalid file/folder name: " + ex.getMessage());
        return "redirect:/files/list";
    }

    @ExceptionHandler(FileException.class)
    public String handleGenericFileException(FileException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "File operation error: " + ex.getMessage());
        return "redirect:/files/list";
    }

    @ExceptionHandler(FolderExeption.class)
    public String handleGenericFileException(FolderExeption ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "Folder operation error: " + ex.getMessage());
        return "redirect:/files/list";
    }

    @ExceptionHandler(FolderDownloadException.class)
    public String handleGenericFileException(FolderDownloadException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "Folder operation error: " + ex.getMessage());
        return "redirect:/files/list";
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred: " + ex.getMessage());
        return "redirect:/files/list";
    }
}
