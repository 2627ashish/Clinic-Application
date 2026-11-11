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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class WhatsAppSessionServiceTest {
    private static final String PHONE = "919810764715";

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private WhatsAppMessagePublisher messagePublisher;

    @Test
    void completesBookingAndPersistsAppointmentWithPendingStatus() {
        WhatsAppSessionService service = service();
        when(patientRepository.findByPhoneNumber("919810764715")).thenReturn(Optional.empty());
        when(patientRepository.findFirstByPhoneNumberEndingWith("9810764715")).thenReturn(Optional.empty());
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> {
            Patient patient = invocation.getArgument(0);
            patient.setId("patient-1");
            return patient;
        });
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment appointment = invocation.getArgument(0);
            appointment.setId("appointment-1");
            return appointment;
        });

        List<List<WhatsAppListRow>> menus = new ArrayList<>();
        doAnswer(invocation -> {
            menus.add(invocation.getArgument(4));
            return null;
        }).when(messagePublisher).publishList(anyString(), anyString(), anyString(), anyString(), anyList());
        service.handle(message("Hi"));
        service.handle(message("Tooth pain since yesterday"));
        service.handle(selection(menus, 0));
        service.handle(selection(menus, 1));
        service.handle(selection(menus, 2, "More times"));
        service.handle(selection(menus, 3, "6:30 PM"));
        service.handle(message("Anita Sharma"));
        service.handle(message("24"));
        service.handle(new WhatsAppInboundMessage(
                "message-id", PHONE, "[Non-text message: interactive]", menus.get(4).get(1).id()));

        ArgumentCaptor<Appointment> appointment = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(appointment.capture());
        assertThat(appointment.getValue().getPatientId()).isEqualTo("patient-1");
        assertThat(appointment.getValue().getStatus()).isEqualTo(AppointmentStatus.PENDING_DOCTOR_APPROVAL);
        assertThat(appointment.getValue().getClinicLocation()).isEqualTo(ClinicLocation.SEC_44);
        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(patientCaptor.capture());
        assertThat(patientCaptor.getValue().getAge()).isEqualTo(24);
        assertThat(patientCaptor.getValue().getGender()).isEqualTo("Female");
        assertThat(appointment.getValue().getChiefComplaintSummary()).isEqualTo("Tooth pain since yesterday");
        assertThat(appointment.getValue().getAppointmentTime())
                .isEqualTo(LocalDate.parse(menus.get(1).get(0).id().substring("book-date|".length()))
                        .atTime(18, 30));

        verify(messagePublisher).publishText(org.mockito.ArgumentMatchers.eq(PHONE),
                org.mockito.ArgumentMatchers.contains("✅ *APPOINTMENT REQUEST RECEIVED*"));
        verify(messagePublisher, times(2)).publishLocation(
                org.mockito.ArgumentMatchers.eq(PHONE),
                org.mockito.ArgumentMatchers.eq(ClinicLocation.SEC_44.label()),
                org.mockito.ArgumentMatchers.eq(ClinicLocation.SEC_44.address()),
                org.mockito.ArgumentMatchers.eq(ClinicLocation.SEC_44.latitude()),
                org.mockito.ArgumentMatchers.eq(ClinicLocation.SEC_44.longitude()));
        assertThat(menus).hasSize(5);
        assertThat(menus.get(0)).extracting(WhatsAppListRow::title)
                .contains("Clinic 1 · Sec-44", "Clinic 2 · Sec-82");
        assertThat(menus.get(2)).hasSizeLessThanOrEqualTo(10)
                .extracting(WhatsAppListRow::title).contains("More times");
        assertThat(menus.get(3)).extracting(WhatsAppListRow::title).contains("6:30 PM");
        assertThat(service.hasActiveSession(PHONE)).isFalse();
    }

    @Test
    void showsDatesAndAvailableTimesAsWhatsAppPickers() {
        WhatsAppSessionService service = service();
        List<List<WhatsAppListRow>> menus = new ArrayList<>();
        doAnswer(invocation -> {
            menus.add(invocation.getArgument(4));
            return null;
        }).when(messagePublisher).publishList(anyString(), anyString(), anyString(), anyString(), anyList());

        service.handle(message("Hi"));
        service.handle(message("Cleaning"));
        service.handle(selection(menus, 0));
        service.handle(selection(menus, 1));

        assertThat(service.hasActiveSession(PHONE)).isTrue();
        assertThat(menus).hasSize(3);
        assertThat(menus.get(1)).isNotEmpty();
        assertThat(menus.get(2)).hasSizeLessThanOrEqualTo(10)
                .extracting(WhatsAppListRow::title).contains("More times");
    }

    @Test
    void answersClinicHoursWithoutStartingBooking() {
        WhatsAppSessionService service = service();

        service.handle(message("What are your hours?"));

        verify(messagePublisher).publishText(org.mockito.ArgumentMatchers.eq(PHONE),
                org.mockito.ArgumentMatchers.contains("10:00 AM-1:00 PM"));
        assertThat(service.hasActiveSession(PHONE)).isFalse();
    }

    @Test
    void bookKeywordDoesNotStartBookingButHiDoes() {
        WhatsAppSessionService service = service();

        service.handle(message("Book"));

        assertThat(service.hasActiveSession(PHONE)).isFalse();
        verify(messagePublisher).publishText(org.mockito.ArgumentMatchers.eq(PHONE),
                org.mockito.ArgumentMatchers.contains("send Hi to start booking"));

        service.handle(message("Hi"));

        assertThat(service.hasActiveSession(PHONE)).isTrue();
        verify(messagePublisher).publishText(org.mockito.ArgumentMatchers.eq(PHONE),
                org.mockito.ArgumentMatchers.contains("What dental concern can we help with?"));
    }

    @Test
    void cancellingBookingPromptsUserToRestartWithHi() {
        WhatsAppSessionService service = service();
        service.handle(message("Hi"));
        assertThat(service.hasActiveSession(PHONE)).isTrue();

        service.handle(message("Cancel booking"));

        assertThat(service.hasActiveSession(PHONE)).isFalse();
        verify(messagePublisher).publishText(org.mockito.ArgumentMatchers.eq(PHONE),
                org.mockito.ArgumentMatchers.contains("To start again, just send Hi."));
    }

    private WhatsAppInboundMessage message(String text) {
        return new WhatsAppInboundMessage("message-id", PHONE, text, "");
    }

    private WhatsAppSessionService service() {
        return new WhatsAppSessionService(patientRepository, appointmentRepository, messagePublisher,
                new WhatsAppBookingOptions(appointmentRepository, messagePublisher));
    }

    private WhatsAppInboundMessage selection(List<List<WhatsAppListRow>> menus, int menuIndex) {
        return new WhatsAppInboundMessage("message-id", PHONE, "[Non-text message: interactive]",
                menus.get(menuIndex).get(0).id());
    }

    private WhatsAppInboundMessage selection(
            List<List<WhatsAppListRow>> menus, int menuIndex, String title
    ) {
        String replyId = menus.get(menuIndex).stream()
                .filter(row -> row.title().equals(title))
                .findFirst()
                .orElseThrow()
                .id();
        return new WhatsAppInboundMessage("message-id", PHONE, "[Non-text message: interactive]", replyId);
    }
}
