package com.chaula.dental.repository;

import com.chaula.dental.domain.Appointment;
import com.chaula.dental.domain.AppointmentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    List<Appointment> findByPatientId(String patientId);

    List<Appointment> findByPatientIdAndDeletedAtIsNull(String patientId);

    Page<Appointment> findByDeletedAtIsNull(Pageable pageable);

    Page<Appointment> findByDeletedAtIsNullAndStatus(AppointmentStatus status, Pageable pageable);

    Page<Appointment> findByDeletedAtIsNullAndConsultationType(
            com.chaula.dental.domain.ConsultationType consultationType,
            Pageable pageable
    );

    Page<Appointment> findByDeletedAtIsNullAndStatusAndConsultationType(
            AppointmentStatus status,
            com.chaula.dental.domain.ConsultationType consultationType,
            Pageable pageable
    );

    Page<Appointment> findByDeletedAtIsNullAndStatusIn(List<AppointmentStatus> statuses, Pageable pageable);

    Page<Appointment> findByDeletedAtIsNullAndStatusInAndConsultationType(
            List<AppointmentStatus> statuses,
            com.chaula.dental.domain.ConsultationType consultationType,
            Pageable pageable
    );
}
