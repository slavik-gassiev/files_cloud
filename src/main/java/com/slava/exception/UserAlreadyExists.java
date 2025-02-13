package com.slava.exception;

public class UserAlreadyExists extends UserException{
    public UserAlreadyExists(String message) {
        super(message);
    }
}
