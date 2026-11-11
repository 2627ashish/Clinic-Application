package com.chaula.dental.domain;

import jakarta.validation.constraints.NotBlank;

public record PrescriptionItem(
        @NotBlank String name,
        @NotBlank String dosage,
        @NotBlank String duration,
        @NotBlank String instructions
) {
}
