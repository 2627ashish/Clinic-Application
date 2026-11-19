package com.chaula.dental.dto;

import com.chaula.dental.domain.PrescriptionItem;

import java.time.Instant;
import java.util.List;

public record PatientVisitSummary(
        String appointmentId,
        Instant visitDate,
        String diagnosis,
        String procedurePerformed,
        List<PrescriptionItem> medicines,
        String aftercareGuidelines
) {
}
