package com.chaula.dental.repository;

import com.chaula.dental.domain.Patient;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.List;

public interface PatientRepository extends MongoRepository<Patient, String> {
    Optional<Patient> findByPhoneNumber(String phone);

    Optional<Patient> findFirstByPhoneNumberEndingWith(String phoneSuffix);

    Optional<Patient> findFirstByPhoneNumberRegex(String phoneRegex);

    List<Patient> findByWhatsappMarketingOptInTrue();

    long countByWhatsappMarketingOptInTrue();
}
