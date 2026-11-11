package com.chaula.dental.repository;

import com.chaula.dental.domain.TreatmentAndBill;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TreatmentAndBillRepository extends MongoRepository<TreatmentAndBill, String> {
    List<TreatmentAndBill> findByPatientIdOrderByCreatedAtDesc(String patientId);

    Optional<TreatmentAndBill> findByAppointmentId(String appointmentId);
}
