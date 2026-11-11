package com.chaula.dental.service;

import com.chaula.dental.domain.Doctor;
import com.chaula.dental.dto.DoctorSessionResponse;
import com.chaula.dental.repository.DoctorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DoctorAuthServiceTest {
    private final DoctorRepository doctors = mock(DoctorRepository.class);
    private final DoctorAuthService service = new DoctorAuthService(doctors);

    @Test
    void createsAccountWithBcryptAndReturnsLoginSession() {
        when(doctors.count()).thenReturn(0L);
        when(doctors.save(any(Doctor.class))).thenAnswer(invocation -> {
            Doctor doctor = invocation.getArgument(0);
            doctor.setId("doctor-id");
            return doctor;
        });

        DoctorSessionResponse login = service.setup(
                "drchauhan", "BootstrapPassword#82", "Dr. B.S. Chauhan", "doctor@example.com");

        var saved = org.mockito.ArgumentCaptor.forClass(Doctor.class);
        org.mockito.Mockito.verify(doctors).save(saved.capture());
        Doctor doctor = saved.getValue();
        assertThat(doctor.getUsername()).isEqualTo("drchauhan");
        assertThat(doctor.getFullName()).isEqualTo("Dr. B.S. Chauhan");
        assertThat(doctor.getEmail()).isEqualTo("doctor@example.com");
        assertThat(doctor.getRole()).isEqualTo("ROLE_DOCTOR");
        assertThat(doctor.getPassword()).startsWith("$2");
        assertThat(new BCryptPasswordEncoder().matches("BootstrapPassword#82", doctor.getPassword())).isTrue();
        assertThat(doctor.isPasswordChangeRequired()).isFalse();
        assertThat(login.passwordChangeRequired()).isFalse();
        service.requireDoctor(login.token());
    }

    @Test
    void refusesSetupWhenAnyDoctorAccountAlreadyExists() {
        when(doctors.count()).thenReturn(1L);

        assertThatThrownBy(() -> service.setup("newdoctor", "MyStrongPassword#2026",
                "Dr. Test", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("setup is already complete");
    }

    @Test
    void changesPasswordAndInvalidatesOldSession() {
        Doctor doctor = Doctor.builder().id("doctor-id").username("drchauhan")
                .password(new BCryptPasswordEncoder().encode("BootstrapPassword#82")).build();
        when(doctors.count()).thenReturn(0L);
        when(doctors.save(any(Doctor.class))).thenAnswer(invocation -> {
            Doctor saved = invocation.getArgument(0);
            saved.setId("doctor-id");
            return saved;
        });
        when(doctors.findByUsername("drchauhan")).thenReturn(Optional.of(doctor));
        when(doctors.findById("doctor-id")).thenReturn(Optional.of(doctor));

        DoctorSessionResponse login = service.setup(
                "drchauhan", "BootstrapPassword#82", "Dr. Chauhan", null);
        service.changePassword(login.token(), "BootstrapPassword#82", "MyNewStrongPassword#2026");

        assertThat(new BCryptPasswordEncoder().matches("MyNewStrongPassword#2026", doctor.getPassword())).isTrue();
        assertThatThrownBy(() -> service.requireDoctor(login.token()))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(service.login("drchauhan", "MyNewStrongPassword#2026").passwordChangeRequired()).isFalse();
    }

    @Test
    void rejectsMalformedStoredPasswordAsUnauthorized() {
        when(doctors.findByUsername("drchauhan"))
                .thenReturn(Optional.of(Doctor.builder()
                        .id("doctor-id")
                        .username("drchauhan")
                        .password("not-a-bcrypt-hash")
                        .build()));

        assertThatThrownBy(() -> service.login("drchauhan", "not-a-bcrypt-hash"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid username or password");
    }
}
