package com.innerview.spring.exception;

public class LanguageAlreadyAssignedException extends RuntimeException {
    public LanguageAlreadyAssignedException(String message) {
        super(message);
    }
}