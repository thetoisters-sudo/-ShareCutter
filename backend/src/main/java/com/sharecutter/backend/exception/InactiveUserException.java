package com.sharecutter.backend.exception;

import com.sharecutter.backend.domain.enums.UserStatus;

public class InactiveUserException extends RuntimeException {

    private final UserStatus status;

    public InactiveUserException(UserStatus status) {
        super("User account is not active");
        this.status = status;
    }

    public InactiveUserException(UserStatus status, String message) {
        super(message);
        this.status = status;
    }

    public UserStatus getStatus() {
        return status;
    }
}