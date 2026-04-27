package com.innerview.spring.exception;

public class LanguageNotAssignedToUserException extends RuntimeException {
    public LanguageNotAssignedToUserException(String message) {
        super(message);
    }
}