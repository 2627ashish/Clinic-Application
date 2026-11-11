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
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class WhatsAppSessionService {
    private static final DateTimeFormatter SLOT_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy, hh:mm a", Locale.ENGLISH);
    private static final String GREETING = "🦷 *CHAUHAN DENTAL CLINIC*\n"
            + "_Your smile, our commitment_\n\n"
            + "Namaste! Welcome to Dr. B.S. Chauhan’s clinic in Noida. 😊\n\n"
            + "Let’s book your visit in a few easy steps.\n"
            + "What dental concern can we help with? (e.g. tooth pain, cleaning, RCT)";

    private final ConcurrentHashMap<String, BookingSession> sessions = new ConcurrentHashMap<>();
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final WhatsAppMessagePublisher messagePublisher;
    private final WhatsAppBookingOptions bookingOptions;

    public void handle(WhatsAppInboundMessage inbound) {
        String phoneNumber = inbound.phoneNumber();
        if (phoneNumber == null || phoneNumber.isBlank()) return;

        sessions.compute(phoneNumber, (phone, current) -> {
            BookingSession session = current == null ? BookingSession.idle() : current;
            String text = inbound.text() == null ? "" : inbound.text().trim();
            String replyId = inbound.replyId() == null ? "" : inbound.replyId().trim();
            String selection = replyId.isBlank() ? text : replyId;
            if (selection.isBlank()) return session;
            if (isCancelCommand(text)) {
                messagePublisher.publishText(phone,
                        "No problem—your current booking steps are cancelled, and no appointment was submitted. "
                                + "To start again, just send Hi.");
                return BookingSession.idle();
            }
            return switch (session.step()) {
                case IDLE -> handleIdle(phone, text);
                case AWAITING_PROBLEM -> captureProblem(phone, text);
                case AWAITING_LOCATION -> captureLocation(phone, selection, session);
                case AWAITING_DATE -> captureDate(phone, selection, session);
                case AWAITING_TIME -> captureTime(phone, selection, session);
                case AWAITING_NAME -> captureName(phone, text, session);
                case AWAITING_AGE -> captureAge(phone, text, session);
                case AWAITING_GENDER -> captureGender(phone, selection, session);
            };
        });
    }

    public boolean hasActiveSession(String phoneNumber) {
        BookingSession session = sessions.get(phoneNumber);
        return session != null && session.step() != BookingSession.Step.IDLE;
    }

    public void clear(String phoneNumber) {
        sessions.remove(phoneNumber);
    }

    private BookingSession handleIdle(String phoneNumber, String text) {
        String query = text.toLowerCase(Locale.ROOT);
        if (containsAny(query, "timing", "hours", "opening", "समय")) {
            messagePublisher.publishText(phoneNumber,
                    "Clinic 1, Sec-44 (Mon-Sat): 10:00 AM-1:00 PM and 5:30 PM-9:00 PM."
                            + "\nClinic 2, Sec-82 (Mon-Sat): 8:30 AM-10:00 AM and 2:00 PM-5:00 PM.");
        } else if (containsAny(query, "address", "location", "where", "पता", "लोकेशन")) {
            messagePublisher.publishText(phoneNumber,
                    "📍 *Our clinic locations*\nTap either location pin below to open it in your maps app.\n"
                            + "Helpline: +91-98107-64715 / +91-97177-73179.");
            bookingOptions.publishLocation(phoneNumber, ClinicLocation.SEC_44);
            bookingOptions.publishLocation(phoneNumber, ClinicLocation.SEC_82);
        } else if (containsAny(query, "pain", "emergency", "toothache", "दर्द", "दाँत दर्द")) {
            messagePublisher.publishText(phoneNumber,
                    "For tooth pain, rinse gently with warm salt water and use a cold compress on the cheek. "
                            + "Avoid putting aspirin or undiluted clove oil on gums. Seek urgent dental care for swelling, fever, or trouble breathing/swallowing. "
                            + "For urgent help call +91-98107-64715 / +91-97177-73179. Send Hi to start booking.");
        } else if (isBookingTrigger(query)) {
            messagePublisher.publishText(phoneNumber, GREETING);
            return BookingSession.awaitingProblem();
        } else {
            messagePublisher.publishText(phoneNumber,
                    "Welcome to Dr. Chauhan Dental Clinic. Just send Hi to start booking, "
                            + "or ask about clinic hours or location.");
        }
        return BookingSession.idle();
    }

    private BookingSession captureProblem(String phoneNumber, String text) {
        bookingOptions.showClinicChoices(phoneNumber);
        return BookingSession.awaitingLocation(text.replaceAll("\\s+", " "));
    }

    private BookingSession captureLocation(String phoneNumber, String text, BookingSession session) {
        ClinicLocation location = text.startsWith("clinic|")
                ? parseClinicLocation(text.substring("clinic|".length()))
                : parseClinicLocation(text);
        if (location == null) {
            bookingOptions.showClinicChoices(phoneNumber);
            return session;
        }
        messagePublisher.publishText(phoneNumber,
                "📍 *" + location.label() + "*\n" + location.address() + "\nTap the location pin to open directions.");
        bookingOptions.publishLocation(phoneNumber, location);
        bookingOptions.showAvailableDates(phoneNumber, location);
        return BookingSession.awaitingDate(session.problem(), location);
    }

    private BookingSession captureDate(String phoneNumber, String replyId, BookingSession session) {
        if (!replyId.startsWith("book-date|")) {
            bookingOptions.showAvailableDates(phoneNumber, session.clinicLocation());
            return session;
        }
        try {
            LocalDate date = LocalDate.parse(replyId.substring("book-date|".length()));
            List<LocalDateTime> slots = bookingOptions.availableSlots(date, session.clinicLocation());
            if (slots.isEmpty()) {
                messagePublisher.publishText(phoneNumber, "Those times are no longer available. Please choose another date.");
                bookingOptions.showAvailableDates(phoneNumber, session.clinicLocation());
                return session;
            }
            bookingOptions.showAvailableTimes(phoneNumber, date, session.clinicLocation(), slots, 0);
            return BookingSession.awaitingTime(session.problem(), session.clinicLocation(), date);
        } catch (java.time.format.DateTimeParseException exception) {
            bookingOptions.showAvailableDates(phoneNumber, session.clinicLocation());
            return session;
        }
    }

    private BookingSession captureTime(String phoneNumber, String replyId, BookingSession session) {
        if (replyId.startsWith("book-more-times|")) {
            showMoreBookingTimes(phoneNumber, replyId, session);
            return session;
        }
        if (!replyId.startsWith("book-time|")) {
            messagePublisher.publishText(phoneNumber, "Please tap one of the available times in the list.");
            return session;
        }
        try {
            LocalDateTime preferredTime = LocalDateTime.parse(replyId.substring("book-time|".length()));
            if (!preferredTime.toLocalDate().equals(session.selectedDate())
                    || !bookingOptions.availableSlots(session.selectedDate(), session.clinicLocation()).contains(preferredTime)) {
                messagePublisher.publishText(phoneNumber, "That time was just booked. Please choose another available time.");
                bookingOptions.showAvailableDates(phoneNumber, session.clinicLocation());
                return BookingSession.awaitingDate(session.problem(), session.clinicLocation());
            }
            messagePublisher.publishText(phoneNumber, "Great choice! 😊 What is your full name?");
            return BookingSession.awaitingName(session.problem(), session.clinicLocation(), preferredTime);
        } catch (java.time.format.DateTimeParseException exception) {
            messagePublisher.publishText(phoneNumber, "Please tap one of the available times in the list.");
            return session;
        }
    }

    private void showMoreBookingTimes(String phoneNumber, String replyId, BookingSession session) {
        String[] parts = replyId.split("\\|", 3);
        if (parts.length != 3 || session.selectedDate() == null) {
            bookingOptions.showAvailableDates(phoneNumber, session.clinicLocation());
            return;
        }
        try {
            LocalDate date = LocalDate.parse(parts[1]);
            int offset = Integer.parseInt(parts[2]);
            if (!date.equals(session.selectedDate())) {
                bookingOptions.showAvailableDates(phoneNumber, session.clinicLocation());
                return;
            }
            List<LocalDateTime> slots = bookingOptions.availableSlots(date, session.clinicLocation());
            if (slots.isEmpty()) {
                bookingOptions.showAvailableDates(phoneNumber, session.clinicLocation());
                return;
            }
            bookingOptions.showAvailableTimes(phoneNumber, date, session.clinicLocation(), slots, offset);
        } catch (java.time.format.DateTimeParseException | NumberFormatException exception) {
            bookingOptions.showAvailableDates(phoneNumber, session.clinicLocation());
        }
    }

    private BookingSession captureName(String phoneNumber, String text, BookingSession session) {
        String name = text.replaceAll("\\s+", " ").trim();
        if (name.length() < 2 || name.length() > 100) {
            messagePublisher.publishText(phoneNumber, "Please enter your name (2-100 characters).");
            return session;
        }
        messagePublisher.publishText(phoneNumber, "Thanks, " + name + "! How old are you? (Enter a number from 1 to 120.)");
        return BookingSession.awaitingAge(
                session.problem(), session.clinicLocation(), session.preferredTime(), name);
    }

    private BookingSession captureAge(String phoneNumber, String text, BookingSession session) {
        int age;
        try {
            age = Integer.parseInt(text.trim());
        } catch (NumberFormatException exception) {
            messagePublisher.publishText(phoneNumber, "Please enter your age as a number from 1 to 120.");
            return session;
        }
        if (age < 1 || age > 120) {
            messagePublisher.publishText(phoneNumber, "Please enter an age from 1 to 120.");
            return session;
        }
        messagePublisher.publishList(phoneNumber, "Please choose your gender.", "Gender", "Select", List.of(
                new WhatsAppListRow("book-gender|Male", "Male", "Select Male"),
                new WhatsAppListRow("book-gender|Female", "Female", "Select Female"),
                new WhatsAppListRow("book-gender|Other", "Other", "Select Other")
        ));
        return BookingSession.awaitingGender(
                session.problem(), session.clinicLocation(), session.preferredTime(), session.patientName(), age);
    }

    private BookingSession captureGender(String phoneNumber, String replyId, BookingSession session) {
        if (!replyId.startsWith("book-gender|")) {
            messagePublisher.publishText(phoneNumber, "Please choose Male, Female, or Other from the list.");
            return session;
        }
        String gender = replyId.substring("book-gender|".length());
        if (!List.of("Male", "Female", "Other").contains(gender)) {
            messagePublisher.publishText(phoneNumber, "Please choose Male, Female, or Other from the list.");
            return session;
        }
        createAppointment(phoneNumber, session, gender);
        return BookingSession.idle();
    }

    private void createAppointment(String phoneNumber, BookingSession session, String gender) {
        String normalizedPhone = PhoneUtils.normalizeIndianPhone(phoneNumber);
        Patient patient = patientRepository.findByPhoneNumber(normalizedPhone)
                .or(() -> patientRepository.findFirstByPhoneNumberEndingWith(lastTenDigits(normalizedPhone)))
                .orElseGet(() -> Patient.builder().phoneNumber(normalizedPhone).build());
        patient.setFullName(session.patientName());
        patient.setAge(session.age());
        patient.setGender(gender);
        Patient savedPatient = patientRepository.save(patient);

        Appointment appointment = appointmentRepository.save(Appointment.builder()
                .patientId(savedPatient.getId())
                .appointmentTime(session.preferredTime())
                .clinicLocation(session.clinicLocation())
                .status(AppointmentStatus.PENDING_DOCTOR_APPROVAL)
                .chiefComplaintSummary(session.problem())
                .build());

        String confirmation = "✅ *APPOINTMENT REQUEST RECEIVED*\n"
                + "━━━━━━━━━━━━━━\n"
                + "👤 *Patient:* " + session.patientName() + "\n"
                + "🗓️ *When:* " + SLOT_FORMAT.format(appointment.getAppointmentTime()) + "\n"
                + "📍 *Where:* " + session.clinicLocation().label() + "\n"
                + "🏠 " + session.clinicLocation().address() + "\n"
                + "━━━━━━━━━━━━━━\n"
                + "Dr. B.S. Chauhan’s team will confirm your visit shortly.\n"
                + "Need help? Call +91-98107-64715 / +91-97177-73179.\n\n"
                + "_Your smile, our commitment_";
        messagePublisher.publishText(phoneNumber, confirmation);
        bookingOptions.publishLocation(phoneNumber, session.clinicLocation());
    }

    private ClinicLocation parseClinicLocation(String text) {
        String query = text.toLowerCase(Locale.ROOT);
        if (query.equals("1") || query.equals("sec_44")
                || containsAny(query, "sec-44", "sector-44", "chhalera", "clinic 1", "clinic1")) {
            return ClinicLocation.SEC_44;
        }
        if (query.equals("2") || query.equals("sec_82")
                || containsAny(query, "sec-82", "sector-82", "madhuban", "clinic 2", "clinic2")) {
            return ClinicLocation.SEC_82;
        }
        return null;
    }

    private boolean isBookingTrigger(String query) {
        return query.equals("hi");
    }

    private boolean isCancelCommand(String text) {
        String command = text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        return command.equals("cancel")
                || command.equals("cancel booking")
                || command.equals("cancel appointment")
                || command.equals("start over");
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) return true;
        }
        return false;
    }

    private String lastTenDigits(String phoneNumber) {
        String digits = phoneNumber.replaceAll("\\D", "");
        return digits.substring(Math.max(0, digits.length() - 10));
    }

}
