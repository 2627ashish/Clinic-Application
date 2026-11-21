package com.chaula.dental.config;

import com.chaula.dental.domain.Patient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatientPhoneIndexMigration implements ApplicationRunner {
    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        mongoTemplate.indexOps(Patient.class).getIndexInfo().stream()
                .filter(IndexInfo::isUnique)
                .filter(index -> index.getIndexFields().stream()
                        .anyMatch(field -> "phoneNumber".equals(field.getKey()))
                        && index.getIndexFields().size() == 1)
                .findFirst()
                .ifPresent(index -> {
                    mongoTemplate.indexOps(Patient.class).dropIndex(index.getName());
                    log.info("Removed the unique patient phone index to support shared family phone numbers.");
                });
    }
}
