package com.innerview.user.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class PasswordAndConfirmationMisMatchException extends RuntimeException {
	public PasswordAndConfirmationMisMatchException(String message) {
		super(message);
	}
}
