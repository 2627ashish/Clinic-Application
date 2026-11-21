package com.chaula.dental.dto;

import java.util.List;

public record AuthResponse(List<PatientSession> patients) {
    public record PatientSession(String token, String patientName) {
    }
}
