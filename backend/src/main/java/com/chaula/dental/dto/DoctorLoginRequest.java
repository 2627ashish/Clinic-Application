package com.chaula.dental.dto;

import jakarta.validation.constraints.NotBlank;

public record DoctorLoginRequest(@NotBlank String username, @NotBlank String password) {
}
