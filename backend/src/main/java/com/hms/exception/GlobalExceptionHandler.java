package com.hms.exception;

import com.hms.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle validation errors (400 Bad Request)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation error on request {}: {}", request.getDescription(false), errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse("Validation failed", errors, false));
    }

    /**
     * Handle access denied (403 Forbidden)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            WebRequest request) {

        log.warn("Access denied for request {}: {}", request.getDescription(false), ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse("Access denied - insufficient permissions", false));
    }

    /**
     * Handle illegal argument exceptions (400 Bad Request)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {

        log.warn("Illegal argument on request {}: {}", request.getDescription(false), ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse("Invalid argument: " + ex.getMessage(), false));
    }

    /**
     * Handle resource not found (404 Not Found)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            WebRequest request) {

        log.warn("Resource not found on request {}: {}", request.getDescription(false), ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(ex.getMessage(), false));
    }

    /**
     * Handle duplicate resource (409 Conflict)
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse> handleDuplicateResourceException(
            DuplicateResourceException ex,
            WebRequest request) {

        log.warn("Duplicate resource on request {}: {}", request.getDescription(false), ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse(ex.getMessage(), false));
    }

    /**
     * Handle operations that are invalid for the resource's current state
     * (409 Conflict) - e.g. occupying a bed in a room that is already full.
     * The request is well formed and the resource exists; the state forbids it.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse> handleIllegalState(
            IllegalStateException ex,
            WebRequest request) {

        log.warn("Invalid state transition on request {}: {}", request.getDescription(false), ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse(ex.getMessage(), false));
    }

    /**
     * Handle database constraint violations (409 Conflict).
     * Catches races where a unique value is inserted between a service's
     * existsBy... check and the actual save.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            WebRequest request) {

        log.warn("Data integrity violation on request {}: {}", request.getDescription(false), ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse("The request conflicts with existing data", false));
    }

    /**
     * Handle malformed request bodies (400 Bad Request)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> handleUnreadableMessage(
            HttpMessageNotReadableException ex,
            WebRequest request) {

        log.warn("Malformed request body on request {}: {}", request.getDescription(false), ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse("Malformed request body", false));
    }

    /**
     * Handle path/query parameters of the wrong type (400 Bad Request),
     * e.g. a non-numeric id in /api/patients/{id}
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            WebRequest request) {

        log.warn("Parameter type mismatch on request {}: parameter '{}'",
                request.getDescription(false), ex.getName());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse("Invalid value for parameter '" + ex.getName() + "'", false));
    }

    /**
     * Handle generic exceptions (500 Internal Server Error).
     * The full exception goes to error.log; the client gets a correlation id
     * instead of internal details.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {

        String requestId = MDC.get("requestId");

        log.error("Unexpected error on request {} [requestId={}]: {}",
                request.getDescription(false), requestId, ex.getMessage(), ex);

        String message = requestId == null
                ? "An unexpected error occurred"
                : "An unexpected error occurred (reference: " + requestId + ")";

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(message, false));
    }
}