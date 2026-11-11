package com.chaula.dental.repository;

import com.chaula.dental.domain.Appointment;
import com.chaula.dental.domain.AppointmentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface AppointmentRepository extends MongoRepository<Appointment, String> {
    List<Appointment> findByStatusOrderByAppointmentTimeAsc(AppointmentStatus status);

    Optional<Appointment> findFirstByPatientIdAndStatusOrderByCreatedAtDesc(
            String patientId,
            AppointmentStatus status
    );

    List<Appointment> findByAppointmentTimeBetween(LocalDateTime start, LocalDateTime end);
}
