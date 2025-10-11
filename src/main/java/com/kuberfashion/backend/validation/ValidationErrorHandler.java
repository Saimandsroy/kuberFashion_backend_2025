package com.kuberfashion.backend.validation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global Exception Handler for Validation Errors
 * Provides consistent error responses aligned with frontend Zod validation
 */
@RestControllerAdvice
public class ValidationErrorHandler {

    /**
     * Handle validation errors from @Valid annotations on request bodies
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        
        Map<String, String> fieldErrors = new HashMap<>();
        
        // Process field errors
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        
        // Process global errors (like password matching)
        bindingResult.getGlobalErrors().forEach(error -> {
            fieldErrors.put("general", error.getDefaultMessage());
        });
        
        ValidationErrorResponse errorResponse = new ValidationErrorResponse(
            "Validation failed",
            "VALIDATION_ERROR",
            fieldErrors
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle constraint violation exceptions (for path variables, request parameters)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> fieldErrors = ex.getConstraintViolations()
            .stream()
            .collect(Collectors.toMap(
                violation -> violation.getPropertyPath().toString(),
                ConstraintViolation::getMessage
            ));
        
        ValidationErrorResponse errorResponse = new ValidationErrorResponse(
            "Validation failed",
            "CONSTRAINT_VIOLATION",
            fieldErrors
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle general validation exceptions
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(ValidationException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        
        if (ex.getFieldErrors() != null) {
            fieldErrors.putAll(ex.getFieldErrors());
        } else {
            fieldErrors.put("general", ex.getMessage());
        }
        
        ValidationErrorResponse errorResponse = new ValidationErrorResponse(
            ex.getMessage(),
            "VALIDATION_ERROR",
            fieldErrors
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Response class for validation errors
     */
    public static class ValidationErrorResponse {
        private String message;
        private String code;
        private Map<String, String> errors;
        private long timestamp;
        private boolean success = false;

        public ValidationErrorResponse(String message, String code, Map<String, String> errors) {
            this.message = message;
            this.code = code;
            this.errors = errors;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters and Setters
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public Map<String, String> getErrors() { return errors; }
        public void setErrors(Map<String, String> errors) { this.errors = errors; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
    }

    /**
     * Custom validation exception class
     */
    public static class ValidationException extends RuntimeException {
        private Map<String, String> fieldErrors;

        public ValidationException(String message) {
            super(message);
        }

        public ValidationException(String message, Map<String, String> fieldErrors) {
            super(message);
            this.fieldErrors = fieldErrors;
        }

        public ValidationException(String field, String message) {
            super(message);
            this.fieldErrors = Map.of(field, message);
        }

        public Map<String, String> getFieldErrors() {
            return fieldErrors;
        }
    }
}
