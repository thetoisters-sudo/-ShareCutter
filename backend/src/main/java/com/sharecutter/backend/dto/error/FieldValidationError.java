package com.sharecutter.backend.dto.error;

public record FieldValidationError(
        String field,
        String message
) {
}