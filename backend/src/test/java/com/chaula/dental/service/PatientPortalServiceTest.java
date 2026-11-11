package com.chaula.dental.service;

import com.chaula.dental.domain.Patient;
import com.chaula.dental.repository.PatientRepository;
import com.chaula.dental.repository.TreatmentAndBillRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatientPortalServiceTest {
    private final PatientRepository patients = mock(PatientRepository.class);
    private final TreatmentAndBillRepository treatments = mock(TreatmentAndBillRepository.class);
    private final WhatsAppMessagePublisher publisher = mock(WhatsAppMessagePublisher.class);
    private final PatientPortalService service =
            new PatientPortalService(patients, treatments, publisher, true);

    @Test
    void verifiesOtpForPatientStoredWithTenDigitPhoneNumber() {
        Patient patient = patient();
        when(patients.findByPhoneNumber("918273161338")).thenReturn(Optional.empty());
        when(patients.findFirstByPhoneNumberEndingWith("8273161338")).thenReturn(Optional.of(patient));

        String code = service.sendOtp("8273161338").demoCode();
        var session = service.verifyOtp("8273161338", code);

        assertThat(session.patientName()).isEqualTo("ashish");
        verify(patients).findFirstByPhoneNumberEndingWith("8273161338");
    }

    @Test
    void matchesFormattedStoredPhoneNumberByDigitsAndReplacesPreviousSession() {
        Patient patient = patient();
        when(patients.findByPhoneNumber("918273161338")).thenReturn(Optional.empty());
        when(patients.findFirstByPhoneNumberEndingWith("8273161338")).thenReturn(Optional.empty());
        when(patients.findFirstByPhoneNumberRegex(anyString())).thenReturn(Optional.of(patient));
        when(patients.findById("patient-1")).thenReturn(Optional.of(patient));
        when(treatments.findByPatientIdOrderByCreatedAtDesc("patient-1")).thenReturn(java.util.List.of());

        String firstCode = service.sendOtp("8273161338").demoCode();
        var firstSession = service.verifyOtp("8273161338", firstCode);
        String secondCode = service.sendOtp("8273161338").demoCode();
        var secondSession = service.verifyOtp("8273161338", secondCode);

        assertThat(secondSession.token()).isNotEqualTo(firstSession.token());
        assertThat(service.visits(secondSession.token())).isEmpty();
        assertThatThrownBy(() -> service.visits(firstSession.token()))
                .isInstanceOf(ResponseStatusException.class);
        verify(patients, org.mockito.Mockito.times(2)).findFirstByPhoneNumberRegex(
                ".*8[^0-9]*2[^0-9]*7[^0-9]*3[^0-9]*1[^0-9]*6[^0-9]*1[^0-9]*3[^0-9]*3[^0-9]*8[^0-9]*$");
    }

    private Patient patient() {
        return Patient.builder().id("patient-1").fullName("ashish").phoneNumber("8273161338").build();
    }
}
