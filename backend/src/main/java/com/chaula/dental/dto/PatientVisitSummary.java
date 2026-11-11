package com.chaula.dental.dto;

import java.time.Instant;

public record PatientVisitSummary(
        String appointmentId,
        Instant visitDate,
        String diagnosis,
        String procedurePerformed
) {
}
