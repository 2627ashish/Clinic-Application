package com.chaula.dental.service;

import com.chaula.dental.domain.Appointment;
import com.chaula.dental.domain.AppointmentStatus;
import com.chaula.dental.domain.ClinicLocation;
import com.chaula.dental.domain.ConsultationType;
import com.chaula.dental.domain.Patient;
import com.chaula.dental.dto.AiIntakeRequest;
import com.chaula.dental.dto.AppointmentResponse;
import com.chaula.dental.repository.AppointmentRepository;
import com.chaula.dental.repository.PatientRepository;
import com.chaula.dental.util.PhoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentIntakeServiceImpl implements AppointmentIntakeService {
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final WhatsAppNotificationService whatsappService;

    @Override
    public AppointmentResponse processAiIntake(AiIntakeRequest request) {
        ConsultationType consultationType = request.consultationType() == null
                ? ConsultationType.IN_CLINIC : request.consultationType();
        if (consultationType == ConsultationType.IN_CLINIC
                && !ClinicSchedule.isAvailable(request.clinicLocation(), request.preferredTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The selected time is outside this clinic's Monday-Saturday shifts.");
        }
        String patientName = request.patientName().trim();
        String phoneNumber;
        try {
            phoneNumber = PhoneUtils.normalizeIndianPhone(request.phoneNumber());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
        Patient patient = PatientIdentityLookup.findMatching(
                        patientRepository, phoneNumber, patientName, request.dateOfBirth())
                .orElseGet(() -> Patient.builder().phoneNumber(phoneNumber).build());
        patient.setPhoneNumber(phoneNumber);
        patient.setFullName(patientName);
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setGender(request.gender().trim());
        List<String> medicalHistory = request.medicalHistory() == null ? List.of() : request.medicalHistory().stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::strip)
                .toList();
        if (medicalHistory.stream().anyMatch(value -> value.startsWith("None /"))) medicalHistory = List.of();
        patient.setMedicalHistory(medicalHistory);
        patient.setOngoingTreatments(cleanOptional(request.ongoingTreatments()));
        boolean eligibleForPregnancyQuestions = patient.getCalculatedAge() >= 18
                && request.gender().equalsIgnoreCase("female");
        patient.setIsPregnant(eligibleForPregnancyQuestions ? request.isPregnant() : null);
        Patient.MedicalDeclaration declaration = request.medicalDeclaration();
        if (declaration != null) {
            patient.setMedicalDeclaration(new Patient.MedicalDeclaration(
                    declaration.hasChronicIllness(),
                    declaration.takingLongTermMedication(),
                    declaration.lignocaineConsent(),
                    declaration.consentToTreatment(),
                    declaration.adherenceConsent(),
                    declaration.hadPreviousDentalTreatment(),
                    declaration.rctCommitment(),
                    eligibleForPregnancyQuestions ? request.isPregnant() : null,
                    eligibleForPregnancyQuestions ? declaration.isBreastfeeding() : null,
                    cleanOptional(declaration.illnessDetails()),
                    cleanOptional(declaration.medicationDetails())
            ));
        }
        if (request.whatsappMarketingOptIn() != null) {
            patient.setWhatsappMarketingOptIn(request.whatsappMarketingOptIn());
            patient.setWhatsappMarketingOptInAt(request.whatsappMarketingOptIn() ? Instant.now() : null);
        }
        Patient savedPatient = patientRepository.save(patient);

        Appointment appointment = Appointment.builder()
                .patientId(savedPatient.getId())
                .appointmentTime(request.preferredTime())
                .clinicLocation(request.clinicLocation())
                .consultationType(consultationType)
                .status(AppointmentStatus.PENDING_DOCTOR_APPROVAL)
                .chiefComplaintSummary(cleanSymptoms(request.symptomsDescription()))
                .dateOfBirth(savedPatient.getDateOfBirth())
                .medicalDeclaration(savedPatient.getMedicalDeclaration())
                .medicalHistory(savedPatient.getMedicalHistory())
                .ongoingTreatments(savedPatient.getOngoingTreatments())
                .isPregnant(savedPatient.getIsPregnant())
                .build();
        Appointment savedAppointment = appointmentRepository.save(appointment);
        if (consultationType == ConsultationType.ONLINE && savedAppointment.getId() != null) {
            savedAppointment.setMeetingLink("https://meet.jit.si/ChauhanDentalClinic-"
                    + savedAppointment.getId().substring(0, Math.min(8, savedAppointment.getId().length()))
                    + "-" + UUID.randomUUID());
            savedAppointment = appointmentRepository.save(savedAppointment);
        }
        whatsappService.sendTextMessageAsync(savedPatient.getPhoneNumber(),
                AppointmentWhatsAppCopy.requestReceived(
                        savedPatient.getFullName(),
                        savedAppointment.getAppointmentTime(),
                        savedAppointment.getClinicLocation(),
                        savedAppointment.getConsultationType()));
        if (consultationType == ConsultationType.IN_CLINIC) {
            whatsappService.sendLocationMessageAsync(
                    savedPatient.getPhoneNumber(), savedAppointment.getClinicLocation());
        }
        return toResponse(savedAppointment, savedPatient);
    }

    @Override
    public List<AppointmentResponse> findPendingAppointments() {
        return appointmentRepository.findByStatusOrderByAppointmentTimeAsc(
                        AppointmentStatus.PENDING_DOCTOR_APPROVAL)
                .stream()
                .map(appointment -> {
                    Patient patient = patientRepository.findById(appointment.getPatientId())
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.NOT_FOUND, "Patient for appointment was not found"));
                    return toResponse(appointment, patient);
                })
                .toList();
    }

    private String cleanSymptoms(String symptoms) {
        return symptoms.strip().replaceAll("\\s+", " ");
    }

    private String cleanOptional(String value) {
        return value == null || value.isBlank() ? null : value.strip().replaceAll("\\s+", " ");
    }

    private AppointmentResponse toResponse(Appointment appointment, Patient patient) {
        return AppointmentResponse.from(appointment, patient);
    }

}
