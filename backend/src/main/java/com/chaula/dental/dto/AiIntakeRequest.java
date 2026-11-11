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

import java.time.LocalDateTime;

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
        Boolean whatsappMarketingOptIn
) {
}
