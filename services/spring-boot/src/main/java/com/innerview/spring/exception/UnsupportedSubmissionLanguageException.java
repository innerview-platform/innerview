package com.innerview.spring.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class UnsupportedSubmissionLanguageException extends RuntimeException {
    private final List<String> supportedLanguages;

    public UnsupportedSubmissionLanguageException(String message, List<String> supportedLanguages) {
        super(message);
        this.supportedLanguages = supportedLanguages;
    }
}
