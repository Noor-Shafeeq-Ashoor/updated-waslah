package com.ga.career_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * Handles application exceptions globally and converts them into
 * consistent HTTP error responses.

 * This keeps error handling centralized instead of duplicating it
 * across individual controllers.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request) {

        log.warn("Resource not found: path={}", request.getRequestURI());

        return buildError(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                exception.getMessage(),
                request
        );
    }


    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException exception,
            HttpServletRequest request) {

        log.warn("Bad request: path={}", request.getRequestURI());

        return buildError(
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST",
                exception.getMessage(),
                request
        );
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField() + ": " + error.getDefaultMessage()
                )
                .findFirst()
                .orElse("Invalid request data");

        log.warn("Request validation failed: path={}",
                request.getRequestURI());

        return buildError(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                message,
                request
        );
    }


    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            ConflictException exception,
            HttpServletRequest request) {

        log.warn("Request conflict: path={}", request.getRequestURI());

        return buildError(
                HttpStatus.CONFLICT,
                "CONFLICT",
                exception.getMessage(),
                request
        );
    }


    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(
            ForbiddenException exception,
            HttpServletRequest request) {

        log.warn("Access forbidden: path={}", request.getRequestURI());

        return buildError(
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                exception.getMessage(),
                request
        );
    }


    @ExceptionHandler({
            AuthorizationDeniedException.class,
            AccessDeniedException.class
    })
    public ResponseEntity<ErrorResponse> handleSpringSecurityForbidden(
            Exception exception,
            HttpServletRequest request) {

        log.warn("Spring Security denied access: path={}",
                request.getRequestURI());

        return buildError(
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "You do not have permission to access this resource",
                request
        );
    }


    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException exception,
            HttpServletRequest request) {

        log.warn("Authentication failed: path={}",
                request.getRequestURI());

        return buildError(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "Invalid username or password",
                request
        );
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception exception,
            HttpServletRequest request) {

        log.error("Unexpected application error: path={}",
                request.getRequestURI(),
                exception);

        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "Something went wrong",
                request
        );
    }

    /**
     * Creates a standardized error response containing the timestamp,
     * HTTP status, error code, message, and request path.
    */
    private ResponseEntity<ErrorResponse> buildError(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request) {

        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                error,
                message,
                request.getRequestURI()
        );

        return new ResponseEntity<>(response, status);
    }
    /**
          * Handles a query or path value of the wrong type, for example an unknown
          * enum value such as ?status=FOO. Without this it would be reported as a 500.
     * */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {

                        log.warn("Invalid value for parameter '{}': path={}",
                                   exception.getName(), request.getRequestURI());
                        return buildError(HttpStatus.BAD_REQUEST,"BAD_REQUEST", "Invalid value for parameter '" + exception.getName() + "'",
                request);
    }



    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException exception,
            HttpServletRequest request) {

        log.warn("Invalid argument: path={}", request.getRequestURI());

        return buildError(
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST",
                exception.getMessage(),
                request
        );
    }


    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledException(
            DisabledException exception,
            HttpServletRequest request) {

        log.warn("Authentication denied: account is inactive");

        return buildError(
                HttpStatus.FORBIDDEN,
                "ACCOUNT_INACTIVE",
                "Your account is inactive and cannot be used",
                request
        );
    }
}