package com.chaula.dental.service;

import com.chaula.dental.domain.ClinicLocation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ClinicScheduleTest {
    @Test
    void keepsClinicSlotsWithinEachLocationSchedule() {
        LocalDate saturday = LocalDate.of(2026, 11, 14);
        assertThat(ClinicSchedule.isAvailable(ClinicLocation.SEC_44, saturday.atTime(12, 30))).isTrue();
        assertThat(ClinicSchedule.isAvailable(ClinicLocation.SEC_44, saturday.atTime(15, 0))).isFalse();
        assertThat(ClinicSchedule.isAvailable(ClinicLocation.SEC_44, saturday.atTime(17, 0))).isFalse();
        assertThat(ClinicSchedule.isAvailable(ClinicLocation.SEC_82, saturday.atTime(8, 30))).isTrue();
        assertThat(ClinicSchedule.isAvailable(ClinicLocation.SEC_82, saturday.atTime(16, 30))).isTrue();
        assertThat(ClinicSchedule.isAvailable(ClinicLocation.SEC_82, saturday.atTime(17, 30))).isFalse();
        assertThat(ClinicSchedule.isAvailable(ClinicLocation.SEC_82,
                LocalDateTime.of(2026, 11, 15, 9, 0))).isFalse();
    }
}
