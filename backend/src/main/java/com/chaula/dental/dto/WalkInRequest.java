package com.chaula.dental.dto;

import com.chaula.dental.domain.ClinicLocation;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record WalkInRequest(
        @NotBlank @Size(max = 100) String patientName,
        @NotBlank @Pattern(regexp = "^(?=(?:\\D*\\d){7,15}\\D*$)\\+?[0-9][0-9() .-]+$") String phoneNumber,
        @NotNull @Min(1) @Max(120) Integer age,
        @NotBlank @Pattern(regexp = "(?i)^(male|female|other)$") String gender,
        @NotNull ClinicLocation clinicLocation,
        @NotBlank @Pattern(regexp = "^(PHONE_CALL|DIRECT_WALK_IN)$") String source,
        @DecimalMin("0.00") BigDecimal quotedFee,
        @Size(max = 1000) String notes
) {
}
