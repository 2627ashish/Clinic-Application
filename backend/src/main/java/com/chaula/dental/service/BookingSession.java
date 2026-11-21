package com.chaula.dental.service;

import com.chaula.dental.domain.ClinicLocation;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingSession(
        Step step,
        String problem,
        ClinicLocation clinicLocation,
        LocalDate selectedDate,
        LocalDateTime preferredTime,
        String patientName,
        LocalDate dateOfBirth
) {
    public enum Step {
        IDLE,
        AWAITING_PROBLEM,
        AWAITING_LOCATION,
        AWAITING_DATE,
        AWAITING_TIME,
        AWAITING_NAME,
        AWAITING_DATE_OF_BIRTH,
        AWAITING_GENDER
    }

    public static BookingSession idle() {
        return new BookingSession(Step.IDLE, "", null, null, null, null, null);
    }

    public static BookingSession awaitingProblem() {
        return new BookingSession(Step.AWAITING_PROBLEM, "", null, null, null, null, null);
    }

    public static BookingSession awaitingLocation(String problem) {
        return new BookingSession(Step.AWAITING_LOCATION, problem, null, null, null, null, null);
    }

    public static BookingSession awaitingDate(String problem, ClinicLocation clinicLocation) {
        return new BookingSession(Step.AWAITING_DATE, problem, clinicLocation, null, null, null, null);
    }

    public static BookingSession awaitingTime(
            String problem, ClinicLocation clinicLocation, LocalDate selectedDate
    ) {
        return new BookingSession(Step.AWAITING_TIME, problem, clinicLocation, selectedDate, null, null, null);
    }

    public static BookingSession awaitingName(
            String problem, ClinicLocation clinicLocation, LocalDateTime preferredTime
    ) {
        return new BookingSession(Step.AWAITING_NAME, problem, clinicLocation,
                preferredTime.toLocalDate(), preferredTime, null, null);
    }

    public static BookingSession awaitingDateOfBirth(
            String problem, ClinicLocation clinicLocation, LocalDateTime preferredTime, String patientName
    ) {
        return new BookingSession(Step.AWAITING_DATE_OF_BIRTH, problem, clinicLocation,
                preferredTime.toLocalDate(), preferredTime, patientName, null);
    }

    public static BookingSession awaitingGender(
            String problem, ClinicLocation clinicLocation, LocalDateTime preferredTime,
            String patientName, LocalDate dateOfBirth
    ) {
        return new BookingSession(Step.AWAITING_GENDER, problem, clinicLocation,
                preferredTime.toLocalDate(), preferredTime, patientName, dateOfBirth);
    }
}
