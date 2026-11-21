package com.chaula.dental.service;

import com.chaula.dental.domain.Patient;
import com.chaula.dental.domain.PatientOtp;
import com.chaula.dental.repository.PatientOtpRepository;
import com.chaula.dental.repository.PatientRepository;
import com.chaula.dental.repository.TreatmentAndBillRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatientAuthServiceImplTest {
    private static final String NORMALIZED_PHONE = "919810764715";

    private final PatientOtpRepository otpRepository = mock(PatientOtpRepository.class);
    private final PatientRepository patients = mock(PatientRepository.class);
    private final TreatmentAndBillRepository treatments = mock(TreatmentAndBillRepository.class);
    private final PatientPortalService patientPortalService = new PatientPortalService(patients, treatments);
    private final WhatsAppMessagePublisher messagePublisher = mock(WhatsAppMessagePublisher.class);
    private final PatientAuthServiceImpl service =
            new PatientAuthServiceImpl(otpRepository, patientPortalService, messagePublisher, true);

    @Test
    void normalizesPhoneAndSendsOtpWithCooldownResponse() {
        when(otpRepository.findByPhoneNumber(NORMALIZED_PHONE)).thenReturn(Optional.empty());
        when(otpRepository.save(any(PatientOtp.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.requestOtp("9810764715");

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("OTP sent to WhatsApp");
        assertThat(response.resendCooldown()).isEqualTo(30);
        assertThat(response.mockOtp()).matches("\\d{4}");
        verify(messagePublisher).publishText(org.mockito.ArgumentMatchers.eq(NORMALIZED_PHONE),
                argThat(message -> message.contains("Your login OTP is: *" + response.mockOtp() + "*")
                        && message.contains("Valid for 10 minutes.")));
    }

    @Test
    void rejectsResendDuringCooldown() {
        PatientOtp otp = otp("1234", Instant.now().plusSeconds(600), Instant.now().minusSeconds(10), 0);
        when(otpRepository.findByPhoneNumber(NORMALIZED_PHONE)).thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> service.requestOtp(NORMALIZED_PHONE))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS))
                .hasMessageContaining("Please wait 30 seconds");
        verify(otpRepository, never()).save(any(PatientOtp.class));
        verify(messagePublisher, never()).publishText(any(), any());
    }

    @Test
    void resendOverwritesCodeAndResetsAttemptCount() {
        PatientOtp previous = otp("old", Instant.now().plusSeconds(600), Instant.now().minusSeconds(35), 4);
        Instant previousSentAt = previous.getLastSentAt();
        AtomicReference<PatientOtp> current = new AtomicReference<>(previous);
        when(otpRepository.findByPhoneNumber(NORMALIZED_PHONE))
                .thenAnswer(invocation -> Optional.of(current.get()));
        when(otpRepository.save(any(PatientOtp.class))).thenAnswer(invocation -> {
            PatientOtp saved = invocation.getArgument(0);
            current.set(saved);
            return saved;
        });

        var response = service.requestOtp(NORMALIZED_PHONE);

        assertThat(current.get().getOtpCode()).isEqualTo(response.mockOtp()).isNotEqualTo("old");
        assertThat(current.get().getVerifyAttempts()).isZero();
        assertThat(current.get().getExpiresAt()).isAfter(Instant.now().plusSeconds(590));
        assertThat(current.get().getLastSentAt()).isAfter(previousSentAt);
        assertThatThrownBy(() -> service.verifyOtp(NORMALIZED_PHONE, "old"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid OTP code");
    }

    @Test
    void deletesOtpAndCreatesSessionAfterSuccessfulSingleUseVerification() {
        PatientOtp otp = otp("4715", Instant.now().plusSeconds(600), Instant.now().minusSeconds(35), 0);
        when(otpRepository.findByPhoneNumber(NORMALIZED_PHONE)).thenReturn(Optional.of(otp));
        Patient patient = Patient.builder().id("patient-1").fullName("Patient").phoneNumber(NORMALIZED_PHONE).build();
        when(patients.findAllByPhoneNumber(NORMALIZED_PHONE)).thenReturn(java.util.List.of(patient));
        when(patients.findById("patient-1")).thenReturn(Optional.of(patient));

        var response = service.verifyOtp(NORMALIZED_PHONE, " 4715 ");
        assertThat(response.patients()).hasSize(1);
        assertThat(response.patients().get(0).patientName()).isEqualTo("Patient");
        assertThat(response.patients().get(0).token()).isNotBlank();

        var order = inOrder(otpRepository, patients);
        order.verify(otpRepository).deleteByPhoneNumber(NORMALIZED_PHONE);
        order.verify(patients).findAllByPhoneNumber(NORMALIZED_PHONE);
    }

    @Test
    void removesOtpAfterMoreThanFiveVerificationAttempts() {
        PatientOtp otp = otp("4715", Instant.now().plusSeconds(600), Instant.now().minusSeconds(35), 5);
        when(otpRepository.findByPhoneNumber(NORMALIZED_PHONE)).thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> service.verifyOtp(NORMALIZED_PHONE, "0000"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("Too many failed attempts");
        verify(otpRepository).deleteByPhoneNumber(NORMALIZED_PHONE);
    }

    @Test
    void rejectsExpiredOtp() {
        PatientOtp otp = otp("4715", Instant.now().minusSeconds(1), Instant.now().minusSeconds(700), 0);
        when(otpRepository.findByPhoneNumber(NORMALIZED_PHONE)).thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> service.verifyOtp(NORMALIZED_PHONE, "4715"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("OTP has expired");
        verify(otpRepository).deleteByPhoneNumber(NORMALIZED_PHONE);
    }

    private PatientOtp otp(String code, Instant expiresAt, Instant lastSentAt, int attempts) {
        return PatientOtp.builder()
                .phoneNumber(NORMALIZED_PHONE)
                .otpCode(code)
                .expiresAt(expiresAt)
                .lastSentAt(lastSentAt)
                .verifyAttempts(attempts)
                .build();
    }
}
