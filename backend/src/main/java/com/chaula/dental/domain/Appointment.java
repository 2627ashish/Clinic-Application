package com.chaula.dental.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "appointments")
public class Appointment {
    @Id
    private String id;

    private String patientId;
    private LocalDateTime appointmentTime;
    private ClinicLocation clinicLocation;
    @Builder.Default
    private ConsultationType consultationType = ConsultationType.IN_CLINIC;
    private String meetingLink;
    private AppointmentStatus status;
    private String chiefComplaintSummary;
    private String doctorNotes;
    private List<String> medicalHistory;
    private String ongoingTreatments;
    private Boolean isPregnant;
    private String intakeSource;
    private BigDecimal quotedFee;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
