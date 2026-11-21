package com.chaula.dental.repository;

import com.chaula.dental.domain.PatientOtp;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PatientOtpRepository extends MongoRepository<PatientOtp, String> {
    Optional<PatientOtp> findByPhoneNumber(String phoneNumber);

    void deleteByPhoneNumber(String phoneNumber);
}
