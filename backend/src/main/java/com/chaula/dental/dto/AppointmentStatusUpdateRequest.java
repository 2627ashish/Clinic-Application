package com.chaula.dental.dto;

import com.chaula.dental.domain.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AppointmentStatusUpdateRequest(
        @NotNull AppointmentStatus status,
        LocalDateTime rescheduledTime
) {
}
