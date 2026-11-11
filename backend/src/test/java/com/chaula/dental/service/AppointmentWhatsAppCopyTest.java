package com.chaula.dental.service;

import com.chaula.dental.domain.AppointmentStatus;
import com.chaula.dental.domain.ClinicLocation;
import com.chaula.dental.domain.ConsultationType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentWhatsAppCopyTest {
    private static final LocalDateTime APPOINTMENT_TIME = LocalDateTime.of(2026, 11, 12, 18, 30);

    @Test
    void requestReceivedCopyIncludesAppointmentAndEmergencyContacts() {
        String message = AppointmentWhatsAppCopy.requestReceived("Asha", APPOINTMENT_TIME, ClinicLocation.SEC_44);

        assertThat(message)
                .contains("Namaste Asha", "Thursday, 12 Nov 2026, 06:30 PM",
                        "Gali No. 3, Chhalera, Sec-44, Noida", "review",
                        "+91-98107-64715 / +91-97177-73179")
                .doesNotContain("template", "https://www.google.com/maps");
    }

    @Test
    void confirmedCopyIncludesArrivalInstructionsAndClinicContacts() {
        String message = AppointmentWhatsAppCopy.statusUpdate(
                AppointmentStatus.CONFIRMED, "Asha", APPOINTMENT_TIME, ClinicLocation.SEC_82);
        assertThat(message).contains("APPOINTMENT IS CONFIRMED", "arrive 10 minutes",
                "Madhuban Apartments, Sec-82", "location separately", "+91-97177-73179")
                .doesNotContain("https://www.google.com/maps");
    }

    @Test
    void rescheduledCopyExplainsChangeAndProvidesBothPhones() {
        String message = AppointmentWhatsAppCopy.statusUpdate(
                AppointmentStatus.RESCHEDULED, "Asha", APPOINTMENT_TIME, ClinicLocation.SEC_44);

        assertThat(message).contains("APPOINTMENT HAS BEEN RESCHEDULED",
                "Thursday, 12 Nov 2026, 06:30 PM", "RESCHEDULE",
                "+91-98107-64715 / +91-97177-73179")
                .doesNotContain("https://www.google.com/maps");
    }

    @Test
    void confirmedOnlineCopyIncludesCallInstructionsAndBackupLink() {
        String message = AppointmentWhatsAppCopy.statusUpdate(
                AppointmentStatus.CONFIRMED,
                "Asha",
                APPOINTMENT_TIME,
                ClinicLocation.SEC_44,
                ConsultationType.ONLINE,
                "https://meet.jit.si/backup-room"
        );

        assertThat(message).contains("ONLINE CONSULTATION CONFIRMED",
                "WhatsApp at the scheduled time",
                "https://meet.jit.si/backup-room")
                .doesNotContain("https://www.google.com/maps");
    }

    @Test
    void onlineRescheduleAndCancellationDoNotIncludeClinicDirections() {
        String rescheduled = AppointmentWhatsAppCopy.statusUpdate(
                AppointmentStatus.RESCHEDULED, "Asha", APPOINTMENT_TIME,
                ClinicLocation.SEC_44, ConsultationType.ONLINE, "https://meet.jit.si/backup");
        String cancelled = AppointmentWhatsAppCopy.statusUpdate(
                AppointmentStatus.CANCELLED, "Asha", APPOINTMENT_TIME,
                ClinicLocation.SEC_44, ConsultationType.ONLINE, null);

        assertThat(rescheduled).contains("WhatsApp video / audio call", "https://meet.jit.si/backup")
                .doesNotContain("https://www.google.com/maps");
        assertThat(cancelled).contains("Online consultation")
                .doesNotContain("https://www.google.com/maps", "Gali No. 3");
    }
}
