package com.chaula.dental.repository;

import com.chaula.dental.domain.PatientDocument;
import com.chaula.dental.domain.PatientDocumentCategory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PatientDocumentRepository extends MongoRepository<PatientDocument, String> {
    List<PatientDocument> findByPatientIdOrderByCreatedAtDesc(String patientId);

    Optional<PatientDocument> findByIdAndPatientId(String id, String patientId);

    Optional<PatientDocument> findByIdAndAppointmentIdAndCategory(
            String id,
            String appointmentId,
            PatientDocumentCategory category
    );

    Optional<PatientDocument> findFirstByAppointmentIdAndPatientIdAndCategoryOrderByCreatedAtDesc(
            String appointmentId,
            String patientId,
            PatientDocumentCategory category
    );

    List<PatientDocument> findByAppointmentId(String appointmentId);

    List<PatientDocument> findByPatientId(String patientId);
}
