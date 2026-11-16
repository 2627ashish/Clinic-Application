package com.chaula.dental.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "patient_documents")
public class PatientDocument {
    @Id
    private String id;
    private String patientId;
    private String appointmentId;
    private String fileName;
    private String contentType;
    private PatientDocumentCategory category;
    private byte[] content;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
