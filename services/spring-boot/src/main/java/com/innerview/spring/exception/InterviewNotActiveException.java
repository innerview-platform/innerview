package com.innerview.spring.exception;

public class InterviewNotActiveException extends RuntimeException {
    public InterviewNotActiveException(String message) {
        super(message);
    }
}
