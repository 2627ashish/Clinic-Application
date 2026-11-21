package com.chaula.dental.dto;

import com.chaula.dental.domain.ClinicLocation;
import com.chaula.dental.domain.ConsultationType;
import com.chaula.dental.domain.Patient;
import com.chaula.dental.domain.Appointment;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

public record AppointmentResponse(
        String id,
        String patientId,
        String patientName,
        String phoneNumber,
        LocalDate dateOfBirth,
        Integer patientAge,
        String gender,
        LocalDateTime appointmentTime,
        ClinicLocation clinicLocation,
        String status,
        String chiefComplaintSummary,
        String doctorNotes,
        List<String> medicalHistory,
        String ongoingTreatments,
        Boolean isPregnant,
        Patient.MedicalDeclaration medicalDeclaration,
        String intakeSource,
        BigDecimal quotedFee,
        Instant createdAt,
        ConsultationType consultationType,
        String meetingLink
) {
    public static AppointmentResponse from(Appointment appointment, Patient patient) {
        return new AppointmentResponse(
                appointment.getId(),
                patient.getId(),
                patient.getFullName(),
                patient.getPhoneNumber(),
                patient.getDateOfBirth(),
                patient.getDateOfBirth() == null ? null : patient.getCalculatedAge(),
                patient.getGender(),
                appointment.getAppointmentTime(),
                ClinicLocation.defaultLocation(appointment.getClinicLocation()),
                appointment.getStatus().name(),
                appointment.getChiefComplaintSummary(),
                appointment.getDoctorNotes(),
                appointment.getMedicalHistory(),
                appointment.getOngoingTreatments(),
                appointment.getIsPregnant(),
                appointment.getMedicalDeclaration(),
                appointment.getIntakeSource(),
                appointment.getQuotedFee(),
                appointment.getCreatedAt(),
                appointment.getConsultationType() == null
                        ? ConsultationType.IN_CLINIC : appointment.getConsultationType(),
                appointment.getMeetingLink()
        );
    }
}
