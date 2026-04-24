package com.innerview.spring.exception;

import com.innerview.spring.dto.ErrorMessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** InterviewSessionExceptionHandler */
@RestControllerAdvice
public class InterviewSessionExceptionHandler {
  @ExceptionHandler(FullRoomException.class)
  public ResponseEntity<ErrorMessageResponse> handleFullRoomException(FullRoomException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new ErrorMessageResponse(e.getMessage()));
  }

  @ExceptionHandler(RoomNotFoundException.class)
  public ResponseEntity<ErrorMessageResponse> handleRoomNotFoundException(RoomNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorMessageResponse(e.getMessage()));
  }

  @ExceptionHandler(ExpiredRoomException.class)
  public ResponseEntity<ErrorMessageResponse> handleExpiredRoomException(ExpiredRoomException e) {
    return ResponseEntity.status(HttpStatus.GONE).body(new ErrorMessageResponse(e.getMessage()));
  }

  @ExceptionHandler(RoomNotReadyException.class)
  public ResponseEntity<ErrorMessageResponse> handleRoomNotReadyException(RoomNotReadyException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new ErrorMessageResponse(e.getMessage()));
  }
}
