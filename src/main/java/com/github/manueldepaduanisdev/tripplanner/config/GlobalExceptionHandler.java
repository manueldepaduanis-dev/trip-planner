package com.github.manueldepaduanisdev.tripplanner.config;

import com.github.manueldepaduanisdev.tripplanner.dto.ErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

/**
 * Global exception handler listening on all Controller
 */
@RestControllerAdvice // Observe all controllers
@Slf4j
public class GlobalExceptionHandler {

    /**
     * QUEUE FULL EXCEPTION
     * @param ex error
     * @param request request HTTP information
     * @return Error DTO
     */
    @ExceptionHandler(TaskRejectedException.class)
    public ResponseEntity<ErrorDTO> handleQueueFull(TaskRejectedException ex, HttpServletRequest request) {
        log.warn("Processing Queue FULL! Request refused from: {}", request.getRequestURI());

        ErrorDTO error = ErrorDTO.builder()
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value()) // 503
                .error("Service is unavailable... it needs a vacation.")
                .message("We're full! Our servers need a vacation, too. Try again soon.")
                .build();

        return ResponseEntity.status((HttpStatus.SERVICE_UNAVAILABLE)).body(error);
    }

    /**
     * NOT FOUND EXCEPTION
     * @param ex error
     * @param request request HTTP information
     * @return Error DTO
     */
    @ExceptionHandler({NoSuchElementException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorDTO> handleNotFound(RuntimeException ex, HttpServletRequest request) {
        log.info("Element not found or not valid ID: {}", ex.getMessage());

        ErrorDTO error = ErrorDTO.builder()
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .status(HttpStatus.NOT_FOUND.value()) // 404
                .error("Not Found")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * GENERIC EXCEPTION
     * @param ex error
     * @param request request HTTP information
     * @return Error DTO
     */
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
