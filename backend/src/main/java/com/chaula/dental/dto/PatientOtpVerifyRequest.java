package com.chaula.dental.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PatientOtpVerifyRequest(
        @NotBlank String phoneNumber,
        @NotBlank @Pattern(regexp = "\\d{4}") String code
) {
}
