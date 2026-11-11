package com.chaula.dental.service;

import com.chaula.dental.domain.Appointment;
import com.chaula.dental.domain.AppointmentStatus;
import com.chaula.dental.domain.ClinicLocation;
import com.chaula.dental.messaging.event.WhatsAppListRow;
import com.chaula.dental.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
class WhatsAppBookingOptions {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE_TITLE = DateTimeFormatter.ofPattern("EEE, dd MMM", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_TITLE = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private final AppointmentRepository appointmentRepository;
    private final WhatsAppMessagePublisher messagePublisher;

    void showClinicChoices(String phoneNumber) {
        messagePublisher.publishList(phoneNumber, "🏥 *Choose your preferred clinic.*",
                "Clinic locations", "Select clinic", List.of(
                        new WhatsAppListRow("clinic|SEC_44", "Clinic 1 · Sec-44",
                                "Chhalera · 10am-1pm, 5:30-9pm"),
                        new WhatsAppListRow("clinic|SEC_82", "Clinic 2 · Sec-82",
                                "Madhuban · 8:30-10am, 2-5pm")
                ));
    }

    void showAvailableDates(String phoneNumber, ClinicLocation location) {
        LocalDate today = LocalDate.now(CLINIC_ZONE);
        List<WhatsAppListRow> rows = new ArrayList<>();
        for (int day = 0; rows.size() < 6 && day < 8; day++) {
            LocalDate date = today.plusDays(day);
            if (!ClinicSchedule.isOpen(date)) continue;
            List<LocalDateTime> slots = availableSlots(date, location);
            if (slots.isEmpty()) continue;
            rows.add(new WhatsAppListRow(
                    "book-date|" + date,
                    DATE_TITLE.format(date),
                    slots.size() + " available appointment times"
            ));
        }
        if (rows.isEmpty()) {
            messagePublisher.publishText(phoneNumber,
                    "No available times were found for the next few days. Please call the clinic for help.");
            return;
        }
        messagePublisher.publishList(phoneNumber, "📅 Select a date at " + location.label() + ".",
                "Available dates", "Choose date", rows);
    }

    void showAvailableTimes(
            String phoneNumber, LocalDate date, ClinicLocation location, List<LocalDateTime> slots, int offset
    ) {
        int start = Math.max(0, Math.min(offset, Math.max(0, slots.size() - 1)));
        int end = Math.min(start + 9, slots.size());
        List<WhatsAppListRow> rows = new ArrayList<>(slots.subList(start, end).stream()
                .map(slot -> new WhatsAppListRow(
                        "book-time|" + slot,
                        TIME_TITLE.format(slot),
                        "Available · " + location.label()
                ))
                .toList());
        if (end < slots.size()) {
            rows.add(new WhatsAppListRow(
                    "book-more-times|" + date + "|" + end,
                    "More times",
                    "See more available times"
            ));
        }
        messagePublisher.publishList(phoneNumber,
                "🕒 Choose an available appointment time for " + DATE_TITLE.format(date) + ".",
                "Available times", "Choose time", rows);
    }

    List<LocalDateTime> availableSlots(LocalDate date, ClinicLocation location) {
        if (!ClinicSchedule.isOpen(date)) return List.of();
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        List<LocalDateTime> occupied = appointmentRepository.findByAppointmentTimeBetween(start, end).stream()
                .filter(appointment -> ClinicLocation.defaultLocation(appointment.getClinicLocation()) == location)
                .filter(appointment -> appointment.getStatus() != AppointmentStatus.CANCELLED
                        && appointment.getStatus() != AppointmentStatus.COMPLETED)
                .map(Appointment::getAppointmentTime)
                .toList();
        LocalDateTime now = LocalDateTime.now(CLINIC_ZONE);
        return ClinicSchedule.slots(location).stream()
                .map(date::atTime)
                .filter(slot -> slot.isAfter(now) && !occupied.contains(slot))
                .toList();
    }

    void publishLocation(String phoneNumber, ClinicLocation location) {
        messagePublisher.publishLocation(phoneNumber, location.label(), location.address(),
                location.latitude(), location.longitude());
    }
}
