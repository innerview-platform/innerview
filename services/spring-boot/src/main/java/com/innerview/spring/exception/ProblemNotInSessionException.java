package com.innerview.spring.exception;

public class ProblemNotInSessionException extends RuntimeException {
    public ProblemNotInSessionException(String message) {
        super(message);
    }
}
