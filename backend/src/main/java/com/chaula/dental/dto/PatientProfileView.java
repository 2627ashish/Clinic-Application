package com.chaula.dental.dto;

import java.time.LocalDate;

public record PatientProfileView(
        String fullName,
        String phoneNumber,
        Integer age,
        LocalDate dateOfBirth,
        String gender
) {
}
