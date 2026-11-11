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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentIntakeServiceImplTest {
    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private WhatsAppNotificationService whatsappService;

    @InjectMocks
    private AppointmentIntakeServiceImpl intakeService;

    @Test
    void updatesExistingPatientAndCreatesPendingAppointment() {
        Patient existingPatient = Patient.builder()
                .id("patient-1")
                .fullName("Old Name")
                .phoneNumber("+919876543210")
                .age(30)
                .build();
        LocalDateTime preferredTime = futureClinicSlot();
        AiIntakeRequest request = new AiIntakeRequest(
                "  New Name  ",
                " +919876543210 ",
                31,
                "Female",
                preferredTime,
                ClinicLocation.SEC_44,
                "  Severe toothache \n since yesterday  ",
                null,
                null
        );
        when(patientRepository.findByPhoneNumber("919876543210")).thenReturn(Optional.empty());
        when(patientRepository.findFirstByPhoneNumberEndingWith("9876543210")).thenReturn(Optional.of(existingPatient));
        when(patientRepository.save(existingPatient)).thenReturn(existingPatient);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment appointment = invocation.getArgument(0);
            appointment.setId("appointment-1");
            return appointment;
        });

        AppointmentResponse response = intakeService.processAiIntake(request);

        assertThat(existingPatient.getFullName()).isEqualTo("New Name");
        assertThat(existingPatient.getAge()).isEqualTo(31);
        assertThat(existingPatient.getGender()).isEqualTo("Female");
        assertThat(existingPatient.getPhoneNumber()).isEqualTo("919876543210");
        assertThat(response.appointmentId()).isEqualTo("appointment-1");
        assertThat(response.patientName()).isEqualTo("New Name");
        assertThat(response.status()).isEqualTo(AppointmentStatus.PENDING_DOCTOR_APPROVAL.name());
        assertThat(response.chiefComplaintSummary()).isEqualTo("Severe toothache since yesterday");
        assertThat(response.appointmentTime()).isEqualTo(preferredTime);
        verify(whatsappService).sendTextMessageAsync(
                "919876543210",
                AppointmentWhatsAppCopy.requestReceived("New Name", preferredTime, ClinicLocation.SEC_44)
        );
        verify(whatsappService).sendLocationMessageAsync("919876543210", ClinicLocation.SEC_44);

        ArgumentCaptor<Appointment> appointmentCaptor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(appointmentCaptor.capture());
        assertThat(appointmentCaptor.getValue().getPatientId()).isEqualTo("patient-1");
    }

    @Test
    void retainsExistingAgeWhenRequestOmitsAge() {
        Patient existingPatient = Patient.builder()
                .id("patient-2")
                .fullName("Patient")
                .phoneNumber("+919876543210")
                .age(27)
                .build();
        AiIntakeRequest request = new AiIntakeRequest(
                "Updated Name",
                "+919876543210",
                27,
                "Other",
                futureClinicSlot(),
                ClinicLocation.SEC_44,
                "Routine cleaning",
                null,
                null
        );
        when(patientRepository.findByPhoneNumber("919876543210")).thenReturn(Optional.empty());
        when(patientRepository.findFirstByPhoneNumberEndingWith("9876543210")).thenReturn(Optional.of(existingPatient));
        when(patientRepository.save(existingPatient)).thenReturn(existingPatient);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        intakeService.processAiIntake(request);

        assertThat(existingPatient.getAge()).isEqualTo(27);
        assertThat(existingPatient.getFullName()).isEqualTo("Updated Name");
    }

    @Test
    void createsPatientWhenPhoneNumberIsNew() {
        AiIntakeRequest request = new AiIntakeRequest(
                "New Patient",
                "+919876543210",
                40,
                "Male",
                futureClinicSlot(),
                ClinicLocation.SEC_44,
                "Routine cleaning",
                null,
                null
        );
        when(patientRepository.findByPhoneNumber("919876543210")).thenReturn(Optional.empty());
        when(patientRepository.findFirstByPhoneNumberEndingWith("9876543210")).thenReturn(Optional.empty());
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> {
            Patient patient = invocation.getArgument(0);
            patient.setId("new-patient");
            return patient;
        });
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment appointment = invocation.getArgument(0);
            appointment.setId("new-appointment");
            return appointment;
        });

        AppointmentResponse response = intakeService.processAiIntake(request);

        assertThat(response.appointmentId()).isEqualTo("new-appointment");
        assertThat(response.patientName()).isEqualTo("New Patient");
        assertThat(response.phoneNumber()).isEqualTo("919876543210");
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    void createsOnlineAppointmentWithBackupRoomAndOnlineNotification() {
        AiIntakeRequest request = new AiIntakeRequest(
                "Online Patient",
                "+919876543210",
                40,
                "Male",
                LocalDateTime.now().plusDays(2).withHour(22),
                ClinicLocation.SEC_44,
                "Tooth pain",
                ConsultationType.ONLINE,
                null
        );
        when(patientRepository.findByPhoneNumber("919876543210")).thenReturn(Optional.empty());
        when(patientRepository.findFirstByPhoneNumberEndingWith("9876543210")).thenReturn(Optional.empty());
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> {
            Patient patient = invocation.getArgument(0);
            patient.setId("online-patient");
            return patient;
        });
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment appointment = invocation.getArgument(0);
            appointment.setId("online-appointment");
            return appointment;
        });

        AppointmentResponse response = intakeService.processAiIntake(request);

        assertThat(response.consultationType()).isEqualTo(ConsultationType.ONLINE);
        assertThat(response.meetingLink()).startsWith("https://meet.jit.si/ChauhanDentalClinic-online-");
        verify(appointmentRepository, org.mockito.Mockito.times(2)).save(any(Appointment.class));
        verify(whatsappService).sendTextMessageAsync(
                org.mockito.ArgumentMatchers.eq("919876543210"),
                org.mockito.ArgumentMatchers.contains("online consultation request")
        );
        org.mockito.Mockito.verify(whatsappService, org.mockito.Mockito.never())
                .sendLocationMessageAsync(org.mockito.ArgumentMatchers.anyString(), any());
    }

    private LocalDateTime futureClinicSlot() {
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SUNDAY) date = date.plusDays(1);
        return date.atTime(11, 0);
    }
}
