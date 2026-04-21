package com.innerview.spring.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.innerview.spring.dto.ErrorMessageResponse;

@RestControllerAdvice
public class UserExceptionHandler {

	@ExceptionHandler(DuplicateEmailException.class)
	public ResponseEntity<ErrorMessageResponse> duplicateEmail(DuplicateEmailException ex) {
		ErrorMessageResponse errorMessageResponse = new ErrorMessageResponse(ex.getMessage());
		return ResponseEntity.status(409).body(errorMessageResponse);
	}

	@ExceptionHandler(PasswordAndConfirmationMisMatchException.class)
	public ResponseEntity<ErrorMessageResponse> passwordAndConfirmMismatch(
			PasswordAndConfirmationMisMatchException ex) {
		ErrorMessageResponse errorMessageResponse =
				new ErrorMessageResponse("Password and confirmation don't match");
		return ResponseEntity.status(400).body(errorMessageResponse);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorMessageResponse> inputValidation(MethodArgumentNotValidException ex) {
		ErrorMessageResponse errorMessageResponse =
				new ErrorMessageResponse(
						ex.getBindingResult().getAllErrors().getFirst().getDefaultMessage());
		return ResponseEntity.status(400).body(errorMessageResponse);
	}

	@ExceptionHandler(InvalidEmailException.class)
	public ResponseEntity<ErrorMessageResponse> invalidEmail(InvalidEmailException ex) {
		ErrorMessageResponse errorMessageResponse = new ErrorMessageResponse(ex.getMessage());

		return ResponseEntity.status(400).body(errorMessageResponse);
	}
	@ExceptionHandler(InvalidRefreshTokenException.class)
	public ResponseEntity<ErrorMessageResponse> invalidRefreshToken(
			InvalidRefreshTokenException ex) {

		return ResponseEntity
				.status(401)
				.body(new ErrorMessageResponse(ex.getMessage()));
	}
	@ExceptionHandler(RefreshTokenExpiredException.class)
	public ResponseEntity<ErrorMessageResponse> refreshTokenExpired(
			RefreshTokenExpiredException ex) {

		return ResponseEntity
				.status(401)
				.body(new ErrorMessageResponse(ex.getMessage()));
	}
	@ExceptionHandler(UserHasProfile.class)
	public ResponseEntity<ErrorMessageResponse> handleUserHasProfile(UserHasProfile ex) {
		ErrorMessageResponse errorMessageResponse = new ErrorMessageResponse(ex.getMessage());
		return ResponseEntity.status(409).body(errorMessageResponse);
	}

	@ExceptionHandler(UserProfileNotFound.class)
	public ResponseEntity<ErrorMessageResponse> handleUserProfileNotFound(UserProfileNotFound ex) {
		ErrorMessageResponse errorMessageResponse = new ErrorMessageResponse(ex.getMessage());
		return ResponseEntity.status(404).body(errorMessageResponse);
	}

	@ExceptionHandler(UserNotFound.class)
	public ResponseEntity<ErrorMessageResponse> handleUserNotFound(UserNotFound ex) {
		ErrorMessageResponse errorMessageResponse = new ErrorMessageResponse(ex.getMessage());
		return ResponseEntity.status(404).body(errorMessageResponse);
	}
	@ExceptionHandler(LanguageNotFoundException.class)
	public ResponseEntity<ErrorMessageResponse> handleLanguageNotFound(LanguageNotFoundException ex) {
		return ResponseEntity.status(404).body(new ErrorMessageResponse(ex.getMessage()));
	}

	@ExceptionHandler(LanguageAlreadyAssignedException.class)
	public ResponseEntity<ErrorMessageResponse> handleLanguageAlreadyAssigned(LanguageAlreadyAssignedException ex) {
		return ResponseEntity.status(409).body(new ErrorMessageResponse(ex.getMessage()));
	}

	@ExceptionHandler(LanguageNotAssignedToUserException.class)
	public ResponseEntity<ErrorMessageResponse> handleLanguageNotAssigned(LanguageNotAssignedToUserException ex) {
		return ResponseEntity.status(404).body(new ErrorMessageResponse(ex.getMessage()));
	}

	@ExceptionHandler(DuplicateLanguageNameException.class)
	public ResponseEntity<ErrorMessageResponse> handleDuplicateLanguageName(DuplicateLanguageNameException ex) {
		return ResponseEntity.status(409).body(new ErrorMessageResponse(ex.getMessage()));
	}
}
