package com.chaula.dental.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "patient_otps")
public class PatientOtp {
    @Id
    private String id;

    @Indexed(unique = true)
    private String phoneNumber;

    private String otpCode;

    @Indexed(expireAfter = "600s")
    private Instant expiresAt;

    private Instant lastSentAt;

    @Builder.Default
    private int verifyAttempts = 0;
}
