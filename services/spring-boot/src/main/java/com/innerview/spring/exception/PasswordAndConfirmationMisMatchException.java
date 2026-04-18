package com.innerview.spring.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class PasswordAndConfirmationMisMatchException extends RuntimeException {
	public PasswordAndConfirmationMisMatchException(String message) {
		super(message);
	}
}
