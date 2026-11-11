package com.chaula.dental.service;

import com.chaula.dental.domain.Appointment;
import com.chaula.dental.domain.AppointmentStatus;
import com.chaula.dental.domain.ClinicLocation;
import com.chaula.dental.domain.ConsultationType;
import com.chaula.dental.domain.Patient;
import com.chaula.dental.domain.TreatmentAndBill;
import com.chaula.dental.dto.AppointmentStatusUpdateRequest;
import com.chaula.dental.dto.AppointmentView;
import com.chaula.dental.dto.ConsultationCompleteRequest;
import com.chaula.dental.repository.AppointmentRepository;
import com.chaula.dental.repository.PatientRepository;
import com.chaula.dental.repository.TreatmentAndBillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final TreatmentAndBillRepository treatmentRepository;
    private final WhatsAppNotificationService whatsappService;
    private final PdfGenerationService pdfService;

    public List<AppointmentView> getAppointments() {
        return appointmentRepository.findAll().stream()
                .map(appointment -> {
                    Patient patient = patientRepository.findById(appointment.getPatientId())
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.NOT_FOUND, "Patient for appointment was not found"));
                    return AppointmentView.from(appointment, patient.getFullName(), patient.getPhoneNumber());
                })
                .toList();
    }

    public Appointment updateStatus(String id, AppointmentStatusUpdateRequest request) {
        Appointment appointment = findAppointment(id);
        if (request.status() == AppointmentStatus.RESCHEDULED) {
            if (request.rescheduledTime() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A new appointment time is required when rescheduling");
            }
            ClinicLocation location = ClinicLocation.defaultLocation(appointment.getClinicLocation());
            if (!request.rescheduledTime().isAfter(LocalDateTime.now(ZoneId.of("Asia/Kolkata")))
                    || !ClinicSchedule.isAvailable(location, request.rescheduledTime())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Choose a future time within the selected clinic's Monday-Saturday shifts.");
            }
            boolean occupied = appointmentRepository.findByAppointmentTimeBetween(
                            request.rescheduledTime(), request.rescheduledTime()).stream()
                    .anyMatch(existing -> !existing.getId().equals(appointment.getId())
                            && ClinicLocation.defaultLocation(existing.getClinicLocation()) == location
                            && existing.getStatus() != AppointmentStatus.CANCELLED
                            && existing.getStatus() != AppointmentStatus.COMPLETED);
            if (occupied) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "That appointment time is already taken at this clinic.");
            }
            appointment.setAppointmentTime(request.rescheduledTime());
            appointment.setClinicLocation(location);
        }
        if (appointment.getConsultationType() == ConsultationType.ONLINE
                && (appointment.getMeetingLink() == null || appointment.getMeetingLink().isBlank())
                && appointment.getId() != null) {
            appointment.setMeetingLink("https://meet.jit.si/ChauhanDentalClinic-"
                    + appointment.getId().substring(0, Math.min(8, appointment.getId().length()))
                    + "-" + UUID.randomUUID());
        }
        appointment.setStatus(request.status());
        Appointment saved = appointmentRepository.save(appointment);
        if (request.status() == AppointmentStatus.CONFIRMED
                || request.status() == AppointmentStatus.RESCHEDULED
                || request.status() == AppointmentStatus.CANCELLED) {
            Patient patient = findPatient(saved.getPatientId());
            whatsappService.sendTextMessageAsync(
                    patient.getPhoneNumber(),
                    AppointmentWhatsAppCopy.statusUpdate(
                            saved.getStatus(),
                            patient.getFullName(),
                            saved.getAppointmentTime(),
                            saved.getClinicLocation(),
                            saved.getConsultationType() == null
                                    ? ConsultationType.IN_CLINIC : saved.getConsultationType(),
                            saved.getMeetingLink())
            );
            if (saved.getConsultationType() != ConsultationType.ONLINE) {
                whatsappService.sendLocationMessageAsync(
                        patient.getPhoneNumber(), saved.getClinicLocation());
            }
        }
        return saved;
    }

    public TreatmentAndBill completeVisit(String id, ConsultationCompleteRequest request) {
        Appointment appointment = findAppointment(id);
        if (request.billItems() == null || request.billItems().isEmpty() || request.billItems().stream()
                .anyMatch(item -> item == null || item.amount() == null || item.amount().signum() < 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "At least one bill item with a valid non-negative amount is required");
        }
        BigDecimal total = request.billItems().stream()
                .map(item -> item.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (request.paidAmount().compareTo(total) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paid amount cannot exceed the bill total");
        }

        Patient patient = findPatient(appointment.getPatientId());
        TreatmentAndBill treatment = treatmentRepository.save(TreatmentAndBill.builder()
                .appointmentId(appointment.getId())
                .patientId(patient.getId())
                .symptomsAndDiagnosis(request.symptomsAndDiagnosis())
                .procedurePerformed(request.procedurePerformed())
                .medicines(request.medicines())
                .aftercareGuidelines(request.aftercareGuidelines())
                .billItems(request.billItems())
                .totalAmount(total)
                .paidAmount(request.paidAmount())
                .build());
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);
        byte[] pdf = pdfService.generateVisitSummaryPdf(patient, treatment);
        whatsappService.sendDocumentAsync(patient.getPhoneNumber(),
                "visit-summary.pdf", pdf);
        return treatment;
    }

    private Appointment findAppointment(String id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
    }

    private Patient findPatient(String id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
    }

}
