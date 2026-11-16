package com.chaula.dental.dto;

import com.chaula.dental.domain.PatientDocument;
import com.chaula.dental.domain.PatientDocumentCategory;

import java.time.Instant;

public record PatientDocumentView(
        String id,
        String appointmentId,
        String fileName,
        String contentType,
        PatientDocumentCategory category,
        Instant createdAt
) {
    public static PatientDocumentView from(PatientDocument document) {
        return new PatientDocumentView(
                document.getId(),
                document.getAppointmentId(),
                document.getFileName(),
                document.getContentType(),
                document.getCategory(),
                document.getCreatedAt()
        );
    }
}
