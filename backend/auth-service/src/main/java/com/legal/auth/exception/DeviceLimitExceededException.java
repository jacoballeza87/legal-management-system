package com.legal.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DeviceLimitExceededException extends RuntimeException {
    public DeviceLimitExceededException(String message) { super(message); }
}
