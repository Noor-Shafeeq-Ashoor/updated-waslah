package com.ga.waslah.exception;

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
 *
 * This keeps error handling centralized instead of duplicating it
 * across individual controllers.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles requests for resources that do not exist.
     *
     * @param exception the exception describing the missing resource
     * @param request the incoming HTTP request
     * @return an HTTP 404 response
     */
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

    /**
     * Handles invalid requests represented by BadRequestException.
     *
     * @param exception the exception containing the error message
     * @param request the incoming HTTP request
     * @return an HTTP 400 response
     */
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

    /**
     * Handles validation failures for request-body fields.
     *
     * Only the first validation error is returned to the client.
     *
     * @param exception the exception containing field validation errors
     * @param request the incoming HTTP request
     * @return an HTTP 400 response with a validation message
     */
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

    /**
     * Handles conflicts, such as duplicate or already-processed requests.
     *
     * @param exception the exception describing the conflict
     * @param request the incoming HTTP request
     * @return an HTTP 409 response
     */
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

    /**
     * Handles application-specific authorization failures.
     *
     * @param exception the exception describing the restriction
     * @param request the incoming HTTP request
     * @return an HTTP 403 response
     */
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

    /**
     * Handles authorization failures raised by Spring Security.
     *
     * A generic message is returned to avoid exposing internal security
     * details to the client.
     *
     * @param exception the authorization exception
     * @param request the incoming HTTP request
     * @return an HTTP 403 response
     */
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

    /**
     * Handles authentication failures caused by invalid credentials.
     *
     * @param exception the authentication exception
     * @param request the incoming HTTP request
     * @return an HTTP 401 response with a generic message
     */
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

    /**
     * Handles unexpected exceptions that have no more specific handler.
     *
     * Internal exception details are logged for troubleshooting, while
     * the client receives a generic error message.
     *
     * @param exception the unexpected exception
     * @param request the incoming HTTP request
     * @return an HTTP 500 response
     */
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
     *
     * @param status the HTTP status to return
     * @param error the application error code
     * @param message the error message
     * @param request the incoming HTTP request
     * @return the response entity containing the error details
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
     * Handles invalid method arguments that cannot be processed.
     *
     * @param exception the exception describing the invalid argument
     * @param request the incoming HTTP request
     * @return an HTTP 400 response
     */
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

    /**
     * Handles authentication failures caused by an inactive account.
     *
     * @param exception the exception indicating that the account is disabled
     * @param request the incoming HTTP request
     * @return an HTTP 403 response
     */
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