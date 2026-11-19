package com.chaula.dental.dto;

public record PatientProfileView(
        String fullName,
        String phoneNumber,
        Integer age,
        String gender
) {
}
