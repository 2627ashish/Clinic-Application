package com.chaula.dental.service;

import com.chaula.dental.domain.AppointmentStatus;
import com.chaula.dental.domain.ClinicLocation;
import com.chaula.dental.domain.ConsultationType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class AppointmentWhatsAppCopy {
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy, hh:mm a", Locale.ENGLISH);
    private static final String CONTACTS = "+91-98107-64715 / +91-97177-73179";

    private AppointmentWhatsAppCopy() {
    }

    public static String requestReceived(
            String patientName,
            LocalDateTime appointmentTime,
            ClinicLocation clinicLocation
    ) {
        return requestReceived(patientName, appointmentTime, clinicLocation, ConsultationType.IN_CLINIC);
    }

    public static String requestReceived(
            String patientName,
            LocalDateTime appointmentTime,
            ClinicLocation clinicLocation,
            ConsultationType consultationType
    ) {
        ClinicLocation location = ClinicLocation.defaultLocation(clinicLocation);
        if (consultationType == ConsultationType.ONLINE) {
            return "🙏 *Namaste " + patientName + "!*\n\n"
                    + "✅ We’ve received your *online consultation request*.\n\n"
                    + "🗓️ *Requested time:* " + format(appointmentTime) + "\n"
                    + "👨‍⚕️ *Doctor:* Dr. B.S. Chauhan\n"
                    + "📱 *Mode:* WhatsApp video / audio call\n\n"
                    + "Our team will review your request and message you once it’s confirmed.\n"
                    + "📞 *Need help?* " + CONTACTS + "\n"
                    + "_Chauhan Dental Clinic · Your smile, our commitment_";
        }
        return "🙏 *Namaste " + patientName + "!*\n\n"
                + "✅ Your appointment request is with our team for review.\n\n"
                + "🗓️ *Requested time:* " + format(appointmentTime) + "\n"
                + "👨‍⚕️ *Doctor:* Dr. B.S. Chauhan\n"
                + "🏥 *Clinic:* " + location.label() + "\n"
                + "📍 *Address:* " + location.address() + "\n\n"
                + "We’ll send you a confirmation as soon as your slot is approved.\n"
                + "📞 *Clinic helpline:* " + CONTACTS + "\n"
                + "_Chauhan Dental Clinic · Your smile, our commitment_";
    }

    public static String statusUpdate(
            AppointmentStatus status,
            String patientName,
            LocalDateTime appointmentTime,
            ClinicLocation clinicLocation
    ) {
        return statusUpdate(status, patientName, appointmentTime, clinicLocation,
                ConsultationType.IN_CLINIC, null);
    }

    public static String statusUpdate(
            AppointmentStatus status,
            String patientName,
            LocalDateTime appointmentTime,
            ClinicLocation clinicLocation,
            ConsultationType consultationType,
            String meetingLink
    ) {
        ClinicLocation location = ClinicLocation.defaultLocation(clinicLocation);
        String dateAndTime = format(appointmentTime);
        if (status == AppointmentStatus.CONFIRMED && consultationType == ConsultationType.ONLINE) {
            return onlineConfirmation(patientName, dateAndTime, meetingLink);
        }
        return switch (status) {
            case CONFIRMED -> inClinicConfirmation(patientName, dateAndTime, location);
            case RESCHEDULED -> rescheduled(patientName, dateAndTime, location, consultationType, meetingLink);
            case CANCELLED -> cancelled(patientName, dateAndTime, location, consultationType);
            default -> throw new IllegalArgumentException("Unsupported appointment notification status: " + status);
        };
    }

    private static String onlineConfirmation(String patientName, String dateAndTime, String meetingLink) {
        return "🎉 *ONLINE CONSULTATION CONFIRMED*\n\n"
                + "Hello *" + patientName + "*, your appointment is all set.\n\n"
                + "🗓️ *Date & time:* " + dateAndTime + "\n"
                + "👨‍⚕️ *Doctor:* Dr. B.S. Chauhan\n"
                + "📱 *How to connect:* Dr. Chauhan will call you on WhatsApp at the scheduled time.\n\n"
                + "💡 Please be somewhere quiet with a good internet connection.\n"
                + (meetingLink == null || meetingLink.isBlank() ? ""
                : "🎥 *Backup video room:* " + meetingLink + "\n")
                + "📞 *Need help?* " + CONTACTS + "\n"
                + "_Chauhan Dental Clinic · Your smile, our commitment_";
    }

    private static String inClinicConfirmation(String patientName, String dateAndTime, ClinicLocation location) {
        return "🎉 *YOUR APPOINTMENT IS CONFIRMED!*\n\n"
                + "Hello *" + patientName + "*, we look forward to seeing you.\n\n"
                + "🗓️ *Date & time:* " + dateAndTime + "\n"
                + "👨‍⚕️ *Doctor:* Dr. B.S. Chauhan\n"
                + "🏥 *Clinic:* " + location.label() + "\n"
                + "📍 *Address:* " + location.address() + "\n\n"
                + "⏰ Please arrive 10 minutes before your appointment.\n"
                + "📍 We’ve sent the clinic location separately—tap it to open directions.\n"
                + "📞 *Need help?* " + CONTACTS + "\n"
                + "_Chauhan Dental Clinic · Your smile, our commitment_";
    }

    private static String rescheduled(
            String patientName, String dateAndTime, ClinicLocation location,
            ConsultationType consultationType, String meetingLink
    ) {
        String details = consultationType == ConsultationType.ONLINE
                ? "📱 *Mode:* WhatsApp video / audio call\n"
                + (meetingLink == null || meetingLink.isBlank() ? "" : "🎥 *Backup video room:* " + meetingLink + "\n")
                : "🏥 *Clinic:* " + location.label() + "\n"
                + "📍 *Address:* " + location.address() + "\n";
        String directions = consultationType == ConsultationType.ONLINE
                ? "" : "📍 We’ve sent the clinic location separately—tap it to open directions.\n";
        return "🗓️ *YOUR APPOINTMENT HAS BEEN RESCHEDULED*\n\n"
                + "Hello *" + patientName + "*, your updated appointment details are below.\n\n"
                + "🕒 *New date & time:* " + dateAndTime + "\n"
                + "👨‍⚕️ *Doctor:* Dr. B.S. Chauhan\n"
                + details + "\n"
                + directions
                + "If this time doesn’t work for you, reply *RESCHEDULE* or call us.\n"
                + "📞 *Clinic helpline:* " + CONTACTS + "\n"
                + "_Chauhan Dental Clinic · Your smile, our commitment_";
    }

    private static String cancelled(
            String patientName, String dateAndTime, ClinicLocation location, ConsultationType consultationType
    ) {
        String details = consultationType == ConsultationType.ONLINE
                ? "💻 *Appointment type:* Online consultation\n"
                : "🏥 *Clinic:* " + location.label() + "\n";
        return "ℹ️ *APPOINTMENT UPDATE*\n\n"
                + "Hello *" + patientName + "*, we’re sorry, but we’re unable to keep your appointment "
                + "scheduled for *" + dateAndTime + "*.\n\n"
                + details
                + "Reply *RESCHEDULE* and we’ll show you other available times.\n"
                + "📞 For urgent concerns, call " + CONTACTS + ".\n"
                + "_Chauhan Dental Clinic · Your smile, our commitment_";
    }

    private static String format(LocalDateTime appointmentTime) {
        return DATE_TIME.format(appointmentTime);
    }
}
