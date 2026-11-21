package com.chaula.dental.dto;

import com.chaula.dental.domain.ClinicLocation;
import com.chaula.dental.domain.ConsultationType;
import com.chaula.dental.domain.Patient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AiIntakeRequest(
        @NotBlank String patientName,
        @NotBlank
        @Pattern(regexp = "^(?=(?:\\D*\\d){7,15}\\D*$)\\+?[0-9][0-9() .-]+$")
        String phoneNumber,
        @NotNull @Past LocalDate dateOfBirth,
        @NotBlank @Pattern(regexp = "(?i)^(male|female|other)$") String gender,
        @NotNull @Future LocalDateTime preferredTime,
        @NotNull ClinicLocation clinicLocation,
        @NotBlank String symptomsDescription,
        ConsultationType consultationType,
        Boolean whatsappMarketingOptIn,
        @Size(max = 6) List<@NotBlank @Size(max = 100) String> medicalHistory,
        @Size(max = 1000) String ongoingTreatments,
        Boolean isPregnant,
        Patient.MedicalDeclaration medicalDeclaration
) {
}
