package com.innerview.spring.exception;

public class DuplicateLanguageNameException extends RuntimeException {
    public DuplicateLanguageNameException(String message) {
        super(message);
    }
}