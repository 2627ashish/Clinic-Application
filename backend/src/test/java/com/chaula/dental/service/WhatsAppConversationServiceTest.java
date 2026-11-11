package com.chaula.dental.service;

import com.chaula.dental.domain.Appointment;
import com.chaula.dental.domain.AppointmentStatus;
import com.chaula.dental.domain.ClinicLocation;
import com.chaula.dental.domain.Patient;
import com.chaula.dental.messaging.event.WhatsAppInboundMessage;
import com.chaula.dental.messaging.event.WhatsAppListRow;
import com.chaula.dental.repository.AppointmentRepository;
import com.chaula.dental.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhatsAppConversationServiceTest {
    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private WhatsAppMessagePublisher messagePublisher;

    @InjectMocks
    private WhatsAppConversationService conversationService;

    @Test
    void answersFeesWithoutInventingAnAmountAndKeepsReplyShort() {
        conversationService.handle(new WhatsAppInboundMessage("msg-1", "919876543210", "What are your fees?", ""));

        ArgumentCaptor<String> response = ArgumentCaptor.forClass(String.class);
        verify(messagePublisher).publishText(eq("919876543210"), response.capture());
        assertThat(response.getValue()).contains("vary by treatment").hasSizeLessThan(300);
    }

    @Test
    void offersShiftBoundedInteractiveTimesForCancelledAppointment() {
        Patient patient = Patient.builder().id("patient-1").phoneNumber("+919876543210").build();
        Appointment cancelled = Appointment.builder()
                .id("appointment-1")
                .patientId(patient.getId())
                .clinicLocation(ClinicLocation.SEC_44)
                .status(AppointmentStatus.CANCELLED)
                .build();
        when(patientRepository.findByPhoneNumber("919876543210")).thenReturn(Optional.empty());
        when(patientRepository.findFirstByPhoneNumberEndingWith("9876543210")).thenReturn(Optional.of(patient));
        when(appointmentRepository.findFirstByPatientIdAndStatusOrderByCreatedAtDesc(
                patient.getId(), AppointmentStatus.CANCELLED)).thenReturn(Optional.of(cancelled));
        when(appointmentRepository.findByAppointmentTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        conversationService.handle(new WhatsAppInboundMessage("msg-2", "919876543210", "RESCHEDULE", ""));

        ArgumentCaptor<List<WhatsAppListRow>> rows = ArgumentCaptor.forClass(List.class);
        verify(messagePublisher).publishList(
                eq("919876543210"),
                contains("Choose a new time"),
                eq("Choose a time"),
                eq("View slots"),
                rows.capture()
        );
        assertThat(rows.getValue()).hasSize(10);
        assertThat(rows.getValue()).allSatisfy(row -> {
            if (row.id().startsWith("reschedule|")) {
                LocalDateTime time = LocalDateTime.parse(row.id().split("\\|", 3)[2]);
                assertThat(time.toLocalDate()).isBetween(
                        LocalDate.now(ZoneId.of("Asia/Kolkata")),
                        LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(5)
                );
                assertThat(time.getMinute()).isIn(0, 30);
                LocalTime timeOfDay = time.toLocalTime();
                boolean insideShift = !timeOfDay.isBefore(LocalTime.of(10, 0))
                        && !timeOfDay.isAfter(LocalTime.of(13, 0))
                        || !timeOfDay.isBefore(LocalTime.of(17, 30))
                        && !timeOfDay.isAfter(LocalTime.of(21, 0));
                assertThat(insideShift).isTrue();
            }
        });
    }
}
