package com.opspilot.opspilotbackend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    GlobalExceptionHandler.class
            );

    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ResponseEntity<Map<String, Object>>
    handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> errors =
                new HashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        Map<String, Object> response =
                createErrorBody(
                        HttpStatus.BAD_REQUEST,
                        "Validation failed"
                );

        response.put("errors", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>>
    handleAccessDeniedException(
            AccessDeniedException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(createErrorBody(
                        HttpStatus.FORBIDDEN,
                        messageOrDefault(
                                exception,
                                "You do not have permission to access this resource"
                        )
                ));
    }

    @ExceptionHandler(
            ResourceNotFoundException.class
    )
    public ResponseEntity<Map<String, Object>>
    handleResourceNotFoundException(
            ResourceNotFoundException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(createErrorBody(
                        HttpStatus.NOT_FOUND,
                        messageOrDefault(
                                exception,
                                "The requested resource was not found"
                        )
                ));
    }

    @ExceptionHandler(
            IllegalArgumentException.class
    )
    public ResponseEntity<Map<String, Object>>
    handleIllegalArgumentException(
            IllegalArgumentException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(createErrorBody(
                        HttpStatus.BAD_REQUEST,
                        messageOrDefault(
                                exception,
                                "The request was invalid"
                        )
                ));
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Map<String, Object>>
    handleExternalServiceException(
            RestClientException exception
    ) {
        logger.error(
                "External service request failed",
                exception
        );

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(createErrorBody(
                        HttpStatus.BAD_GATEWAY,
                        "OpsPilot could not reach the required external service"
                ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>>
    handleRuntimeException(
            RuntimeException exception
    ) {
        logger.warn(
                "API request could not be completed: {}",
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(createErrorBody(
                        HttpStatus.BAD_REQUEST,
                        messageOrDefault(
                                exception,
                                "The request could not be completed"
                        )
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>>
    handleUnexpectedException(
            Exception exception
    ) {
        logger.error(
                "Unexpected API error",
                exception
        );

        return ResponseEntity
                .status(
                        HttpStatus.INTERNAL_SERVER_ERROR
                )
                .body(createErrorBody(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Something went wrong"
                ));
    }

    private Map<String, Object> createErrorBody(
            HttpStatus status,
            String message
    ) {
        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "timestamp",
                LocalDateTime.now()
        );

        response.put(
                "status",
                status.value()
        );

        response.put(
                "message",
                message
        );

        return response;
    }

    private String messageOrDefault(
            Exception exception,
            String fallback
    ) {
        String message = exception.getMessage();

        if (message == null ||
                message.isBlank()) {
            return fallback;
        }

        return message;
    }
}