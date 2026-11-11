package com.chaula.dental.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "patients")
public class Patient {
    @Id
    private String id;

    private String fullName;

    @Indexed(unique = true)
    private String phoneNumber;

    private Integer age;
    private String gender;

    @Builder.Default
    private boolean whatsappMarketingOptIn = false;
    private Instant whatsappMarketingOptInAt;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
