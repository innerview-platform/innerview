package com.innerview.spring.exception;

public class UserHasProfile extends RuntimeException {
    public UserHasProfile(String message) {
        super(message);
    }
}
