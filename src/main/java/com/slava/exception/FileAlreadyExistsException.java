package com.slava.exception;

public class FileAlreadyExistsException extends FileException {
    public FileAlreadyExistsException(String message) {
        super(message);
    }
}
