package com.github.manueldepaduanisdev.tripplanner.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ErrorDTO {
    private LocalDateTime timestamp;
    private String path;
    private int status;
    private String error;
    private String message;
}
