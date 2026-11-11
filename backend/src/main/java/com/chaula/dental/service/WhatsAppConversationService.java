package com.chaula.dental.service;

import com.chaula.dental.domain.Appointment;
import com.chaula.dental.domain.AppointmentStatus;
import com.chaula.dental.domain.ClinicLocation;
import com.chaula.dental.domain.Patient;
import com.chaula.dental.messaging.event.WhatsAppInboundMessage;
import com.chaula.dental.messaging.event.WhatsAppListRow;
import com.chaula.dental.repository.AppointmentRepository;
import com.chaula.dental.repository.PatientRepository;
import com.chaula.dental.util.PhoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WhatsAppConversationService {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter SLOT_TITLE = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.ENGLISH);
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final WhatsAppMessagePublisher messagePublisher;

    public void handle(WhatsAppInboundMessage inbound) {
        String text = inbound.text() == null ? "" : inbound.text().trim();
        String replyId = inbound.replyId() == null ? "" : inbound.replyId().trim();
        if (replyId.startsWith("reschedule|")) {
            applyReschedule(inbound.phoneNumber(), replyId);
        } else if (replyId.startsWith("more|")) {
            showMoreSlots(inbound.phoneNumber(), replyId);
        } else if (text.equalsIgnoreCase("RESCHEDULE")) {
            beginReschedule(inbound.phoneNumber());
        } else {
            replyToQuestion(inbound.phoneNumber(), text);
        }
    }

    private void replyToQuestion(String phoneNumber, String text) {
        String query = text.toLowerCase(Locale.ROOT);
        boolean hindi = text.codePoints().anyMatch(character -> character >= 0x0900 && character <= 0x097F);
        String response;
        if (containsAny(query, "available today", "available", "open today", "doctor today", "appointment today", "आज", "उपलब्ध")) {
            response = hindi
                    ? "सेक्टर-44: सुबह 10–1 और शाम 5:30–9 बजे। सेक्टर-82: सुबह 8:30–10 और दोपहर 2–5 बजे।"
                    : "Clinic 1 (Sec-44, Mon-Sat): 10 AM–1 PM and 5:30 PM–9 PM. "
                            + "Clinic 2 (Sec-82, Mon-Sat): 8:30 AM–10 AM and 2 PM–5 PM.";
        } else if (containsAny(query, "fee", "fees", "price", "cost", "charge")) {
            response = hindi
                    ? "फीस उपचार और आपकी स्थिति पर निर्भर करती है। अपनी परेशानी बताएं; जाँच के बाद क्लिनिक सही फीस की पुष्टि करेगा।"
                    : "Fees vary by treatment and case. Share your dental concern and the clinic can confirm the applicable fee after assessment.";
        } else if (containsAny(query, "address", "location", "where", "पता", "लोकेशन")) {
            response = "Clinic 1: Gali No. 3, Chhalera, Sec-44, Noida."
                    + "\nClinic 2: 78A MIG, Madhuban Apartments, Sec-82, Noida."
                    + "\n📍 Location pins are below. Tap a pin to open directions."
                    + "\nCall +91-98107-64715 / +91-97177-73179.";
        } else if (containsAny(query, "pain", "toothache", "tooth ache", "दर्द", "दाँत")) {
            response = hindi
                    ? "दाँत दर्द में गुनगुने नमक-पानी से कुल्ला करें और गाल पर ठंडी सिकाई करें। मसूड़ों पर एस्पिरिन न रखें। सूजन, बुखार या साँस/निगलने में परेशानी हो तो तुरंत मदद लें।"
                    : "For tooth pain, rinse gently with warm salt water and use a cold pack on the cheek. Don’t put aspirin on gums. "
                            + "Swelling, fever, or trouble breathing/swallowing needs urgent care. "
                            + "Emergency helpline: +91-98107-64715 / +91-97177-73179.";
        } else {
            response = hindi
                    ? "चौहान डेंटल क्लिनिक को संदेश देने के लिए धन्यवाद। अपॉइंटमेंट बदलने के लिए RESCHEDULE लिखें। अन्य सवालों के लिए क्लिनिक टीम संपर्क करेगी।"
                    : "Thanks for messaging Chauhan Dental Clinic. For appointment changes, reply RESCHEDULE. For other questions, the clinic team will follow up during clinic hours.";
        }
        messagePublisher.publishText(phoneNumber, response);
        if (containsAny(query, "address", "location", "where", "पता", "लोकेशन")) {
            publishLocation(phoneNumber, ClinicLocation.SEC_44);
            publishLocation(phoneNumber, ClinicLocation.SEC_82);
        }
    }

    private void beginReschedule(String phoneNumber) {
        Optional<Patient> patient = findPatient(phoneNumber);
        if (patient.isEmpty()) {
            messagePublisher.publishText(phoneNumber,
                    "We could not match this number to a patient record. Please contact the clinic to arrange a reschedule.");
            return;
        }
        Optional<Appointment> cancelled = appointmentRepository
                .findFirstByPatientIdAndStatusOrderByCreatedAtDesc(
                        patient.get().getId(), AppointmentStatus.CANCELLED);
        if (cancelled.isEmpty()) {
            messagePublisher.publishText(phoneNumber,
                    "There is no cancelled appointment awaiting rescheduling. The clinic team can help with other changes.");
            return;
        }
        showSlots(phoneNumber, cancelled.get(), 0);
    }

    private void showMoreSlots(String phoneNumber, String replyId) {
        String[] parts = replyId.split("\\|", 3);
        if (parts.length != 3) return;
        Optional<Appointment> appointment = findOwnedCancelledAppointment(phoneNumber, parts[1]);
        if (appointment.isEmpty()) return;
        try {
            showSlots(phoneNumber, appointment.get(), Integer.parseInt(parts[2]));
        } catch (NumberFormatException exception) {
            messagePublisher.publishText(phoneNumber, "That time list expired. Reply RESCHEDULE to see current options.");
        }
    }

    private void showSlots(String phoneNumber, Appointment appointment, int offset) {
        ClinicLocation location = ClinicLocation.defaultLocation(appointment.getClinicLocation());
        List<LocalDateTime> slots = availableSlots(location);
        if (slots.isEmpty()) {
            messagePublisher.publishText(phoneNumber, "No future appointment times are available in the next 6 days. Please contact the clinic.");
            return;
        }
        if (offset < 0 || offset >= slots.size()) offset = 0;
        int end = Math.min(offset + 9, slots.size());
        List<WhatsAppListRow> rows = new ArrayList<>();
        for (LocalDateTime slot : slots.subList(offset, end)) {
            rows.add(new WhatsAppListRow(
                    "reschedule|" + appointment.getId() + "|" + slot,
                    SLOT_TITLE.format(slot),
                    location.label()
            ));
        }
        if (end < slots.size()) {
            rows.add(new WhatsAppListRow(
                    "more|" + appointment.getId() + "|" + end,
                    "More times",
                    "See the next available slots"
            ));
        }
        messagePublisher.publishList(
                phoneNumber,
                "Choose a new time at " + location.label() + ". All listed times match its clinic shifts.",
                "Choose a time",
                "View slots",
                rows
        );
    }

    private void applyReschedule(String phoneNumber, String replyId) {
        String[] parts = replyId.split("\\|", 3);
        if (parts.length != 3) return;
        Optional<Appointment> appointment = findOwnedCancelledAppointment(phoneNumber, parts[1]);
        if (appointment.isEmpty()) {
            messagePublisher.publishText(phoneNumber, "That reschedule request is no longer active. Reply RESCHEDULE for current options.");
            return;
        }
        LocalDateTime selected;
        try {
            selected = LocalDateTime.parse(parts[2]);
        } catch (Exception exception) {
            messagePublisher.publishText(phoneNumber, "That time is invalid. Reply RESCHEDULE to see current options.");
            return;
        }
        Appointment updated = appointment.get();
        ClinicLocation location = ClinicLocation.defaultLocation(updated.getClinicLocation());
        if (!availableSlots(location).contains(selected)) {
            messagePublisher.publishText(phoneNumber, "That time is no longer available. Reply RESCHEDULE to choose another slot.");
            return;
        }
        updated.setAppointmentTime(selected);
        updated.setStatus(AppointmentStatus.RESCHEDULED);
        appointmentRepository.save(updated);
        messagePublisher.publishText(phoneNumber, "✅ *Appointment rescheduled*\n"
                + "🗓️ " + SLOT_TITLE.format(selected) + "\n"
                + "📍 " + location.label() + "\n"
                + location.address() + "\n"
                + "For help, call +91-98107-64715 / +91-97177-73179.");
        publishLocation(phoneNumber, location);
    }

    private Optional<Appointment> findOwnedCancelledAppointment(String phoneNumber, String appointmentId) {
        Optional<Patient> patient = findPatient(phoneNumber);
        if (patient.isEmpty()) return Optional.empty();
        return appointmentRepository.findById(appointmentId)
                .filter(appointment -> appointment.getPatientId().equals(patient.get().getId()))
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.CANCELLED);
    }

    private Optional<Patient> findPatient(String phoneNumber) {
        try {
            String normalized = PhoneUtils.normalizeIndianPhone(phoneNumber);
            return patientRepository.findByPhoneNumber(normalized)
                    .or(() -> patientRepository.findFirstByPhoneNumberEndingWith(
                            normalized.substring(normalized.length() - 10)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private List<LocalDateTime> availableSlots(ClinicLocation location) {
        LocalDateTime now = LocalDateTime.now(CLINIC_ZONE);
        LocalDateTime end = LocalDateTime.of(now.toLocalDate().plusDays(7), LocalTime.MAX);
        List<LocalDateTime> occupied = appointmentRepository.findByAppointmentTimeBetween(now, end).stream()
                .filter(appointment -> ClinicLocation.defaultLocation(appointment.getClinicLocation()) == location)
                .filter(appointment -> appointment.getStatus() != AppointmentStatus.CANCELLED
                        && appointment.getStatus() != AppointmentStatus.COMPLETED)
                .map(Appointment::getAppointmentTime)
                .toList();
        LocalDate today = now.toLocalDate();
        List<LocalDateTime> slots = new ArrayList<>();
        int openDays = 0;
        for (int day = 0; openDays < 6; day++) {
            LocalDate date = today.plusDays(day);
            if (!ClinicSchedule.isOpen(date)) continue;
            openDays++;
            for (LocalTime time : ClinicSchedule.slots(location)) {
                LocalDateTime slot = LocalDateTime.of(date, time);
                if (slot.isAfter(now) && !occupied.contains(slot)) slots.add(slot);
            }
        }
        return slots;
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) return true;
        }
        return false;
    }

    private void publishLocation(String phoneNumber, ClinicLocation location) {
        messagePublisher.publishLocation(phoneNumber, location.label(), location.address(),
                location.latitude(), location.longitude());
    }
}
