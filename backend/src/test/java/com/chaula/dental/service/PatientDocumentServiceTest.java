package com.chaula.dental.service;

import com.chaula.dental.domain.PatientDocument;
import com.chaula.dental.domain.PatientDocumentCategory;
import com.chaula.dental.repository.AppointmentRepository;
import com.chaula.dental.repository.PatientDocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatientDocumentServiceTest {
    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final PatientDocumentRepository documents = mock(PatientDocumentRepository.class);
    private final PatientDocumentService service = new PatientDocumentService(appointments, documents);

    @Test
    void findsTheLatestHandwrittenImageForTheAuthenticatedPatientAndVisit() {
        PatientDocument document = PatientDocument.builder()
                .id("rx-1")
                .appointmentId("appointment-1")
                .patientId("patient-1")
                .category(PatientDocumentCategory.HANDWRITTEN_RX)
                .contentType("image/png")
                .content(new byte[] {1, 2, 3})
                .build();
        when(documents.findFirstByAppointmentIdAndPatientIdAndCategoryOrderByCreatedAtDesc(
                "appointment-1", "patient-1", PatientDocumentCategory.HANDWRITTEN_RX))
                .thenReturn(Optional.of(document));

        assertThat(service.findHandwrittenForPatient("appointment-1", "patient-1"))
                .isSameAs(document);
        verify(documents).findFirstByAppointmentIdAndPatientIdAndCategoryOrderByCreatedAtDesc(
                "appointment-1", "patient-1", PatientDocumentCategory.HANDWRITTEN_RX);
    }

    @Test
    void reportsMissingHandwrittenImageAsNotFound() {
        when(documents.findFirstByAppointmentIdAndPatientIdAndCategoryOrderByCreatedAtDesc(
                "appointment-1", "patient-1", PatientDocumentCategory.HANDWRITTEN_RX))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findHandwrittenForPatient("appointment-1", "patient-1"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
