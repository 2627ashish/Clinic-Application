package com.chaula.dental.service;

import com.chaula.dental.domain.Patient;
import com.chaula.dental.repository.PatientRepository;
import com.chaula.dental.repository.TreatmentAndBillRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatientPortalServiceTest {
    private final PatientRepository patients = mock(PatientRepository.class);
    private final TreatmentAndBillRepository treatments = mock(TreatmentAndBillRepository.class);
    private final PatientPortalService service = new PatientPortalService(patients, treatments);

    @Test
    void createsSessionForPatientStoredWithTenDigitPhoneNumber() {
        Patient patient = patient();
        when(patients.findAllByPhoneNumber("918273161338")).thenReturn(List.of());
        when(patients.findByPhoneNumberEndingWith("8273161338")).thenReturn(List.of(patient));
        when(patients.findById("patient-1")).thenReturn(Optional.of(patient));

        var response = service.createSessionsForPhone("918273161338");

        assertThat(response.patients()).hasSize(1);
        assertThat(response.patients().get(0).patientName()).isEqualTo("ashish");
        verify(patients).findByPhoneNumberEndingWith("8273161338");
    }

    @Test
    void createsSeparateSessionsForAllFamilyProfilesSharingPhoneNumber() {
        Patient parent = Patient.builder().id("parent").fullName("Parent").phoneNumber("919876543210").build();
        Patient child = Patient.builder().id("child").fullName("Child").phoneNumber("919876543210").build();
        when(patients.findAllByPhoneNumber("919876543210")).thenReturn(List.of(parent, child));

        var response = service.createSessionsForPhone("919876543210");

        assertThat(response.patients()).extracting("patientName").containsExactly("Parent", "Child");
        assertThat(response.patients()).extracting("token").doesNotHaveDuplicates();
    }

    @Test
    void matchesFormattedStoredPhoneNumberAndReplacesPreviousSession() {
        Patient patient = patient();
        when(patients.findAllByPhoneNumber("918273161338")).thenReturn(List.of());
        when(patients.findByPhoneNumberEndingWith("8273161338")).thenReturn(List.of());
        when(patients.findByPhoneNumberRegex(anyString())).thenReturn(List.of(patient));
        when(patients.findById("patient-1")).thenReturn(Optional.of(patient));
        when(treatments.findByPatientIdOrderByCreatedAtDesc("patient-1")).thenReturn(List.of());

        var firstSession = service.createSessionsForPhone("918273161338").patients().get(0);
        var secondSession = service.createSessionsForPhone("918273161338").patients().get(0);

        assertThat(secondSession.token()).isNotEqualTo(firstSession.token());
        assertThat(service.visits(secondSession.token())).isEmpty();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.visits(firstSession.token()))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verify(patients, org.mockito.Mockito.times(2)).findByPhoneNumberRegex(
                ".*8[^0-9]*2[^0-9]*7[^0-9]*3[^0-9]*1[^0-9]*6[^0-9]*1[^0-9]*3[^0-9]*3[^0-9]*8[^0-9]*$");
    }

    @Test
    void returnsAuthenticatedPatientProfileDetails() {
        Patient patient = Patient.builder()
                .id("patient-1")
                .fullName("ashish")
                .phoneNumber("8273161338")
                .dateOfBirth(LocalDate.now().minusYears(34).minusDays(1))
                .gender("Male")
                .build();
        when(patients.findAllByPhoneNumber("918273161338")).thenReturn(List.of());
        when(patients.findByPhoneNumberEndingWith("8273161338")).thenReturn(List.of(patient));
        when(patients.findById("patient-1")).thenReturn(Optional.of(patient));

        String token = service.createSessionsForPhone("918273161338").patients().get(0).token();

        var profile = service.profile(token);
        assertThat(profile.fullName()).isEqualTo("ashish");
        assertThat(profile.phoneNumber()).isEqualTo("8273161338");
        assertThat(profile.age()).isEqualTo(34);
        assertThat(profile.gender()).isEqualTo("Male");
    }

    private Patient patient() {
        return Patient.builder().id("patient-1").fullName("ashish").phoneNumber("8273161338").build();
    }
}
