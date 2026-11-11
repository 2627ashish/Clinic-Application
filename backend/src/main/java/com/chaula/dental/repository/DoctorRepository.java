package com.chaula.dental.repository;

import com.chaula.dental.domain.Doctor;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DoctorRepository extends MongoRepository<Doctor, String> {
    Optional<Doctor> findByUsername(String username);
}
