package com.chaula.dental.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "patients")
public class Patient {
    @Id
    private String id;

    private String fullName;

    private String phoneNumber;

    private LocalDate dateOfBirth;
    private String gender;
    private List<String> medicalHistory;
    private String ongoingTreatments;
    private Boolean isPregnant;
    private MedicalDeclaration medicalDeclaration;

    @Builder.Default
    private boolean whatsappMarketingOptIn = false;
    private Instant whatsappMarketingOptInAt;

    @Builder.Default
    private Instant createdAt = Instant.now();

    public int getCalculatedAge() {
        return dateOfBirth == null ? 0 : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    public Integer getAge() {
        return dateOfBirth == null ? null : getCalculatedAge();
    }

    public record MedicalDeclaration(
            boolean hasChronicIllness,
            boolean takingLongTermMedication,
            boolean lignocaineConsent,
            boolean consentToTreatment,
            boolean adherenceConsent,
            boolean hadPreviousDentalTreatment,
            boolean rctCommitment,
            Boolean isPregnant,
            Boolean isBreastfeeding,
            String illnessDetails,
            String medicationDetails
    ) {
    }
}
