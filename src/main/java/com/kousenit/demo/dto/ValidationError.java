package com.kousenit.demo.dto;

public record ValidationError(
        String field,
        Object rejectedValue,
        String message,
        String code
){
    public static ValidationError of(String field, Object rejectedValue, String message, String code) {
        return new ValidationError(field, rejectedValue, message, code);
    }
}