package com.github.manueldepaduanisdev.tripplanner.config;

import com.github.manueldepaduanisdev.tripplanner.dto.ErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // --- 503 SERVICE UNAVAILABLE ---
    @ExceptionHandler(TaskRejectedException.class)
    public ResponseEntity<ErrorDTO> handleQueueFull(TaskRejectedException ex, HttpServletRequest request) {
        log.warn("Processing Queue FULL! Request refused from: {}", request.getRequestURI());

        ErrorDTO error = ErrorDTO.builder()
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value()) // 503
                .error("Service Unavailable")
                .message("We're full! Our servers need a vacation, too. Try again soon.")
                .build();

        return ResponseEntity.status((HttpStatus.SERVICE_UNAVAILABLE)).body(error);
    }

    // --- 404 NOT FOUND ---
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorDTO> handleNotFound(NoSuchElementException ex, HttpServletRequest request) {
        log.info("Element not found: {}", ex.getMessage());

        ErrorDTO error = ErrorDTO.builder()
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .status(HttpStatus.NOT_FOUND.value()) //404
                .error("Not Found")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // --- 400 BAD REQUEST (Per IllegalArgumentException) ---
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDTO> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Invalid Argument: {}", ex.getMessage());

        ErrorDTO error = ErrorDTO.builder()
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .status(HttpStatus.BAD_REQUEST.value()) // 400
                .error("Bad Request")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorDTO> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        log.warn("Request error: {} - {}", ex.getStatusCode(), ex.getReason());

        ErrorDTO error = ErrorDTO.builder()
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .status(ex.getStatusCode().value())
                .error(((HttpStatus) ex.getStatusCode()).getReasonPhrase())
                .message(ex.getReason())
                .build();

        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }

    // --- 500 INTERNAL SERVER ERROR (Catch-All) ---
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDTO> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected server error", ex);

        ErrorDTO error = ErrorDTO.builder()
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value()) // 500
                .error("Internal Server Error")
                .message("An unexpected internal error occurred.")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}