package com.chaula.dental.service;

import com.chaula.dental.domain.ClinicLocation;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class ClinicSchedule {
    private static final LocalTime[][] SEC_44_SHIFTS = {
            {LocalTime.of(10, 0), LocalTime.of(13, 0)},
            {LocalTime.of(17, 30), LocalTime.of(21, 0)}
    };
    private static final LocalTime[][] SEC_82_SHIFTS = {
            {LocalTime.of(8, 30), LocalTime.of(10, 0)},
            {LocalTime.of(14, 0), LocalTime.of(17, 0)}
    };

    private ClinicSchedule() {
    }

    public static boolean isAvailable(ClinicLocation location, LocalDateTime dateTime) {
        return isOpen(dateTime.toLocalDate()) && slots(location).contains(dateTime.toLocalTime());
    }

    public static boolean isOpen(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    public static List<LocalTime> slots(ClinicLocation location) {
        LocalTime[][] shifts = ClinicLocation.defaultLocation(location) == ClinicLocation.SEC_82
                ? SEC_82_SHIFTS : SEC_44_SHIFTS;
        List<LocalTime> slots = new ArrayList<>();
        for (LocalTime[] shift : shifts) {
            for (LocalTime time = shift[0]; !time.isAfter(shift[1]); time = time.plusMinutes(30)) {
                slots.add(time);
            }
        }
        return List.copyOf(slots);
    }
}
