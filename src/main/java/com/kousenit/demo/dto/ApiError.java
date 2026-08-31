package com.kousenit.demo.dto;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ApiError(
        LocalDateTime timeStamp,
        int status,
        String error,
        String message,
        String Path,
        List<ValidationError> validationErrors
){
    public static ApiError withValidationErrors(
            HttpStatus status,
            String message,
            String path,
            List<ValidationError> validationErrors
    ){
        return new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                validationErrors
        );
    }
}
