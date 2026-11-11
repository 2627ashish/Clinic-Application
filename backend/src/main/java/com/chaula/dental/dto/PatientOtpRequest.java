package com.chaula.dental.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PatientOtpRequest(
        @NotBlank @Pattern(regexp = "^(?=(?:\\D*\\d){10,12}\\D*$)[0-9+() .-]+$") String phoneNumber
) {
}
