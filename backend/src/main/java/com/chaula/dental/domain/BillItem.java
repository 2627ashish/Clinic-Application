package com.chaula.dental.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record BillItem(
        @NotBlank String description,
        @NotNull @PositiveOrZero BigDecimal amount
) {
}
