package com.innerview.spring.exception;

import com.innerview.spring.dto.ErrorMessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProblemExceptionHandler {
    @ExceptionHandler(ProblemNotFoundException.class)
    public ResponseEntity<ErrorMessageResponse> handleProblemNotFoundException(ProblemNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorMessageResponse(e.getMessage()));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorMessageResponse> handleProblemAuthorizationDeniedException(AuthorizationDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorMessageResponse(e.getMessage()));
    }

    @ExceptionHandler(ProblemRestorationException.class)
    public ResponseEntity<ErrorMessageResponse> handleProblemRestorationException(ProblemRestorationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorMessageResponse(e.getMessage()));
    }

    @ExceptionHandler(TestCaseNotFoundException.class)
    public ResponseEntity<ErrorMessageResponse> handleTestCaseNotFoundException(TestCaseNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorMessageResponse(e.getMessage()));
    }
}
