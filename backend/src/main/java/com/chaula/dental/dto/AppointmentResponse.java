package com.chaula.dental.dto;

import com.chaula.dental.domain.ClinicLocation;
import com.chaula.dental.domain.ConsultationType;
import java.time.Instant;
import java.time.LocalDateTime;

public record AppointmentResponse(
        String appointmentId,
        String patientName,
        String phoneNumber,
        LocalDateTime appointmentTime,
        ClinicLocation clinicLocation,
        String status,
        String chiefComplaintSummary,
        Instant createdAt,
        ConsultationType consultationType,
        String meetingLink
) {
}
