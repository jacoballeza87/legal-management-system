package com.legal.cases.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CaseNotFoundException extends RuntimeException {
    public CaseNotFoundException(String message) { super(message); }
}
