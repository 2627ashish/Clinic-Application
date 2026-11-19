package com.chaula.dental.dto;

import com.chaula.dental.domain.Appointment;
import com.chaula.dental.domain.AppointmentStatus;
import com.chaula.dental.domain.ClinicLocation;
import com.chaula.dental.domain.ConsultationType;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

public record AppointmentView(
        String id,
        String patientId,
        String patientName,
        String phoneNumber,
        Integer patientAge,
        LocalDateTime appointmentTime,
        ClinicLocation clinicLocation,
        ConsultationType consultationType,
        String meetingLink,
        AppointmentStatus status,
        String chiefComplaintSummary,
        String doctorNotes,
        List<String> medicalHistory,
        String ongoingTreatments,
        Boolean isPregnant,
        String intakeSource,
        BigDecimal quotedFee
) {
    public static AppointmentView from(Appointment appointment, String patientName, String phoneNumber, Integer patientAge) {
        return new AppointmentView(
                appointment.getId(),
                appointment.getPatientId(),
                patientName,
                phoneNumber,
                patientAge,
                appointment.getAppointmentTime(),
                ClinicLocation.defaultLocation(appointment.getClinicLocation()),
                appointment.getConsultationType() == null
                        ? ConsultationType.IN_CLINIC : appointment.getConsultationType(),
                appointment.getMeetingLink(),
                appointment.getStatus(),
                appointment.getChiefComplaintSummary(),
                appointment.getDoctorNotes(),
                appointment.getMedicalHistory(),
                appointment.getOngoingTreatments(),
                appointment.getIsPregnant(),
                appointment.getIntakeSource(),
                appointment.getQuotedFee()
        );
    }
}
