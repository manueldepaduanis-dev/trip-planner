package com.github.manueldepaduanisdev.tripplanner.dto.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Status {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED
}
