package com.chaula.dental.dto;

import com.chaula.dental.domain.BillItem;
import com.chaula.dental.domain.PrescriptionItem;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public record ConsultationCompleteRequest(
        @NotBlank
        String symptomsAndDiagnosis,
        @NotBlank
        String procedurePerformed,
        List<@Valid PrescriptionItem> medicines,
        @NotBlank
        String aftercareGuidelines,
        @NotEmpty List<@Valid BillItem> billItems,
        @NotNull @PositiveOrZero BigDecimal paidAmount
) {
}
