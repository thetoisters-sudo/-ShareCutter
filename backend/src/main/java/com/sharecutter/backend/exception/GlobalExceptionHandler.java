package com.sharecutter.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(
            UserNotFoundException exception,
            HttpServletRequest request
    ) {
        ApiError apiError = createApiError(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(apiError);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleUserAlreadyExists(
            UserAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        ApiError apiError = createApiError(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(apiError);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(
            InvalidCredentialsException exception,
            HttpServletRequest request
    ) {
        ApiError apiError = createApiError(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(apiError);
    }

    @ExceptionHandler(InactiveUserException.class)
    public ResponseEntity<ApiError> handleInactiveUser(
            InactiveUserException exception,
            HttpServletRequest request
    ) {
        ApiError apiError = createApiError(
                HttpStatus.FORBIDDEN,
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(apiError);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .distinct()
                .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = "Request validation failed";
        }

        ApiError apiError = createApiError(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(apiError);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        ApiError apiError = createApiError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(apiError);
    }

    private ApiError createApiError(
            HttpStatus status,
            String message,
            String path
    ) {
        return new ApiError(
                OffsetDateTime.now(ZoneOffset.UTC),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
    }

    private String formatFieldError(FieldError fieldError) {
        String defaultMessage = fieldError.getDefaultMessage();

        if (defaultMessage == null || defaultMessage.isBlank()) {
            defaultMessage = "is invalid";
        }

        return fieldError.getField() + ": " + defaultMessage;
    }
}