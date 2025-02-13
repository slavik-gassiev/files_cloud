package com.slava.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FileNotFoundException.class)
    public String handleFileNotFound(FileNotFoundException ex, Model model) {
        model.addAttribute("errorMessage", "File/Folder not found: " + ex.getMessage());
        return "error";
    }

    @ExceptionHandler(FileAlreadyExistsException.class)
    public String handleFileAlreadyExists(FileAlreadyExistsException ex, Model model) {
        model.addAttribute("errorMessage", "File/Folder already exists: " + ex.getMessage());
        return "error";
    }

    @ExceptionHandler(InvalidFileNameException.class)
    public String handleInvalidFileName(InvalidFileNameException ex, Model model) {
        model.addAttribute("errorMessage", "Invalid file/folder name: " + ex.getMessage());
        return "error";
    }

    @ExceptionHandler(FileDownLoadException.class)
    public String handleFileDownloadException(FileDownLoadException ex, Model model) {
        model.addAttribute("errorMessage", "File download error: " + ex.getMessage());
        return "error";
    }

    @ExceptionHandler(FileException.class)
    public String handleFileException(FileException ex, Model model) {
        model.addAttribute("errorMessage", "File operation error: " + ex.getMessage());
        return "error";
    }

    @ExceptionHandler(FolderException.class)
    public String handleFolderException(FolderException ex, Model model) {
        model.addAttribute("errorMessage", "Folder operation error: " + ex.getMessage());
        return "error";
    }

    @ExceptionHandler(FolderDownloadException.class)
    public String handleFolderDownloadException(FolderDownloadException ex, Model model) {
        model.addAttribute("errorMessage", "Folder download error: " + ex.getMessage());
        return "error";
    }

    @ExceptionHandler(UserException.class)
    public String handleUserException(UserException ex, Model model) {
        model.addAttribute("errorMessage", "User operation error: " + ex.getMessage());
        return "error";
    }

    @ExceptionHandler(UserAlreadyExists.class)
    public String handleUserAlreadyExists(UserAlreadyExists ex, Model model) {
        model.addAttribute("errorMessage", "User operation error: " + ex.getMessage());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, Model model) {
        model.addAttribute("errorMessage", "An unexpected error occurred: " + ex.getMessage());
        return "error";
    }
}
