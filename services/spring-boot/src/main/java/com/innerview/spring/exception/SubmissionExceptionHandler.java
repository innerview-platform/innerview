package com.innerview.spring.exception;

import com.innerview.spring.dto.ErrorMessageResponse;
import com.innerview.spring.dto.UnsupportedLanguageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SubmissionExceptionHandler {

    @ExceptionHandler(InterviewNotFoundException.class)
    public ResponseEntity<ErrorMessageResponse> handleInterviewNotFound(InterviewNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessageResponse(exception.getMessage()));
    }

    @ExceptionHandler(InterviewNotActiveException.class)
    public ResponseEntity<ErrorMessageResponse> handleInterviewNotActive(InterviewNotActiveException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorMessageResponse(exception.getMessage()));
    }

    @ExceptionHandler(ProblemNotInSessionException.class)
    public ResponseEntity<ErrorMessageResponse> handleProblemNotInSession(ProblemNotInSessionException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorMessageResponse(exception.getMessage()));
    }

    @ExceptionHandler(SubmissionNotFoundException.class)
    public ResponseEntity<ErrorMessageResponse> handleSubmissionNotFound(SubmissionNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessageResponse(exception.getMessage()));
    }

    @ExceptionHandler(UnsupportedSubmissionLanguageException.class)
    public ResponseEntity<UnsupportedLanguageResponse> handleUnsupportedLanguage(UnsupportedSubmissionLanguageException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new UnsupportedLanguageResponse(exception.getMessage(), exception.getSupportedLanguages()));
    }
}
