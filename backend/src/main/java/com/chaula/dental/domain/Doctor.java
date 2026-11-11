package com.chaula.dental.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "doctors")
public class Doctor {
    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private String password;
    private String fullName;
    private String email;
    private String role;

    @Builder.Default
    private Instant createdAt = Instant.now();

    private boolean passwordChangeRequired;

    @Field("passwordHash")
    private String legacyPasswordHash;
}
