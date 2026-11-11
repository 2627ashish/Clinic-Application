package com.chaula.dental.config;

import com.chaula.dental.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MongoConnectionTest implements CommandLineRunner {

    private final PatientRepository patientRepository;

    @Override
    public void run(String... args) {
        log.info("Checking MongoDB connection...");
        try {
            long count = patientRepository.count();
            log.info("MongoDB Connected Successfully! Current patient count: {}", count);
        } catch (Exception e) {
            log.error("Failed to connect to MongoDB: {}", e.getMessage());
        }
    }
}