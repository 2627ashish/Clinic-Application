package com.chaula.dental.repository;

import com.chaula.dental.domain.PatientDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PatientDocumentRepository extends MongoRepository<PatientDocument, String> {
    List<PatientDocument> findByPatientIdOrderByCreatedAtDesc(String patientId);

    Optional<PatientDocument> findByIdAndPatientId(String id, String patientId);
}
