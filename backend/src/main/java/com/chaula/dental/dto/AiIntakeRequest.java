package com.chaula.dental.dto;

import com.chaula.dental.domain.ClinicLocation;
import com.chaula.dental.domain.ConsultationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record AiIntakeRequest(
        @NotBlank String patientName,
        @NotBlank
        @Pattern(regexp = "^(?=(?:\\D*\\d){7,15}\\D*$)\\+?[0-9][0-9() .-]+$")
        String phoneNumber,
        @NotNull @Min(1) @Max(120) Integer age,
        @NotBlank @Pattern(regexp = "(?i)^(male|female|other)$") String gender,
        @NotNull @Future LocalDateTime preferredTime,
        @NotNull ClinicLocation clinicLocation,
        @NotBlank String symptomsDescription,
        ConsultationType consultationType,
        Boolean whatsappMarketingOptIn,
        @Size(max = 6) List<@NotBlank @Size(max = 100) String> medicalHistory,
        @Size(max = 1000) String ongoingTreatments,
        Boolean isPregnant
) {
    public AiIntakeRequest(
            String patientName,
            String phoneNumber,
            Integer age,
            String gender,
            LocalDateTime preferredTime,
            ClinicLocation clinicLocation,
            String symptomsDescription,
            ConsultationType consultationType,
            Boolean whatsappMarketingOptIn
    ) {
        this(patientName, phoneNumber, age, gender, preferredTime, clinicLocation,
                symptomsDescription, consultationType, whatsappMarketingOptIn, List.of(), null, null);
    }
}
