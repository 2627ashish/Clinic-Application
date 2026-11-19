package com.chaula.dental.service;

import com.chaula.dental.domain.Appointment;
import com.chaula.dental.domain.BillItem;
import com.chaula.dental.domain.Patient;
import com.chaula.dental.domain.PatientDocument;
import com.chaula.dental.domain.PatientDocumentCategory;
import com.chaula.dental.domain.TreatmentAndBill;
import com.chaula.dental.dto.ConsultationCompleteRequest;
import com.chaula.dental.repository.AppointmentRepository;
import com.chaula.dental.repository.PatientDocumentRepository;
import com.chaula.dental.repository.PatientRepository;
import com.chaula.dental.repository.TreatmentAndBillRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentServiceTest {
    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final PatientRepository patients = mock(PatientRepository.class);
    private final PatientDocumentRepository documents = mock(PatientDocumentRepository.class);
    private final TreatmentAndBillRepository treatments = mock(TreatmentAndBillRepository.class);
    private final WhatsAppNotificationService whatsapp = mock(WhatsAppNotificationService.class);
    private final PdfGenerationService pdf = mock(PdfGenerationService.class);
    private final AppointmentService service =
            new AppointmentService(appointments, patients, documents, treatments, whatsapp, pdf);

    @Test
    void sendsHandwrittenPrescriptionAlongsideVisitSummaryWhenCompletingVisit() {
        Appointment appointment = Appointment.builder().id("appointment-1").patientId("patient-1").build();
        Patient patient = Patient.builder()
                .id("patient-1")
                .fullName("Patient")
                .phoneNumber("919999999999")
                .build();
        PatientDocument handwritten = PatientDocument.builder()
                .id("document-1")
                .appointmentId("appointment-1")
                .fileName("handwritten-prescription.png")
                .contentType("image/png")
                .category(PatientDocumentCategory.HANDWRITTEN_RX)
                .content(new byte[]{1, 2, 3})
                .build();
        ConsultationCompleteRequest request = new ConsultationCompleteRequest(
                "Diagnosis",
                "Procedure",
                List.of(),
                "Aftercare",
                List.of(new BillItem("Consultation", BigDecimal.valueOf(500))),
                BigDecimal.ZERO,
                "document-1"
        );
        when(appointments.findById("appointment-1")).thenReturn(Optional.of(appointment));
        when(patients.findById("patient-1")).thenReturn(Optional.of(patient));
        when(documents.findByIdAndAppointmentIdAndCategory(
                "document-1", "appointment-1", PatientDocumentCategory.HANDWRITTEN_RX))
                .thenReturn(Optional.of(handwritten));
        when(treatments.save(any(TreatmentAndBill.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pdf.generateVisitSummaryPdf(any(Patient.class), any(TreatmentAndBill.class)))
                .thenReturn(new byte[]{4, 5, 6});

        service.completeVisit("appointment-1", request);

        verify(whatsapp).sendDocumentAsync("919999999999", "visit-summary.pdf", new byte[]{4, 5, 6});
        verify(whatsapp).sendImageAsync(
                "919999999999",
                "Your handwritten prescription from Dr. B.S. Chauhan.",
                "handwritten-prescription.png",
                "image/png",
                new byte[]{1, 2, 3}
        );
    }
}
