package com.chaula.dental.service;

import com.chaula.dental.domain.Doctor;
import com.chaula.dental.dto.DoctorSessionResponse;
import com.chaula.dental.repository.DoctorRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class DoctorAuthService {
    private static final long SESSION_SECONDS = 8 * 60 * 60;
    private final DoctorRepository doctors;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();
    private final Map<String, DoctorSession> sessions = new ConcurrentHashMap<>();

    public DoctorAuthService(DoctorRepository doctors) {
        this.doctors = doctors;
    }

    public boolean setupRequired() {
        return doctors.count() == 0;
    }

    public synchronized DoctorSessionResponse setup(String username, String password, String fullName, String email) {
        if (!setupRequired()) {
            throw new ResponseStatusException(CONFLICT,
                    "Doctor setup is already complete. Sign in with the existing account.");
        }
        Doctor doctor = doctors.save(Doctor.builder().username(username.trim())
                .password(passwords.encode(password))
                .fullName(fullName == null || fullName.isBlank() ? "Dr. B.S. Chauhan" : fullName.trim())
                .email(email == null || email.isBlank() ? null : email.trim())
                .role("ROLE_DOCTOR")
                .passwordChangeRequired(false)
                .build());
        String token = newToken();
        sessions.put(digest(token), new DoctorSession(doctor.getId(),
                Instant.now().plusSeconds(SESSION_SECONDS), false));
        return new DoctorSessionResponse(token, false);
    }

    public DoctorSessionResponse login(String suppliedUsername, String password) {
        Doctor doctor = doctors.findByUsername(suppliedUsername)
                .filter(account -> matches(password, storedPassword(account)))
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid username or password"));
        if (doctor.getPassword() == null && doctor.getLegacyPasswordHash() != null) {
            doctor.setPassword(doctor.getLegacyPasswordHash());
            doctor.setLegacyPasswordHash(null);
            doctors.save(doctor);
        }
        String token = newToken();
        sessions.put(digest(token), new DoctorSession(doctor.getId(),
                Instant.now().plusSeconds(SESSION_SECONDS), doctor.isPasswordChangeRequired()));
        return new DoctorSessionResponse(token, doctor.isPasswordChangeRequired());
    }

    public void requireDoctor(String token) {
        DoctorSession session = session(token);
        if (session.passwordChangeRequired()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Change the initial password before continuing");
        }
    }

    public void changePassword(String token, String currentPassword, String newPassword) {
        DoctorSession session = session(token);
        Doctor doctor = doctors.findById(session.doctorId())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Doctor account no longer exists"));
        if (!matches(currentPassword, storedPassword(doctor))) {
            throw new ResponseStatusException(UNAUTHORIZED, "Current password is incorrect");
        }
        doctor.setPassword(passwords.encode(newPassword));
        doctor.setLegacyPasswordHash(null);
        doctor.setPasswordChangeRequired(false);
        doctors.save(doctor);
        sessions.entrySet().removeIf(entry -> entry.getValue().doctorId().equals(doctor.getId()));
    }

    private String storedPassword(Doctor doctor) {
        return doctor.getPassword() != null ? doctor.getPassword() : doctor.getLegacyPasswordHash();
    }

    private boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) return false;
        try {
            return passwords.matches(rawPassword, encodedPassword);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private DoctorSession session(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Doctor login is required");
        }
        DoctorSession session = sessions.get(digest(token));
        if (session == null || !session.expiresAt().isAfter(Instant.now())) {
            sessions.remove(digest(token));
            throw new ResponseStatusException(UNAUTHORIZED, "Doctor session expired; please log in again");
        }
        return session;
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String digest(String token) {
        try {
            return Base64.getEncoder().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private record DoctorSession(String doctorId, Instant expiresAt, boolean passwordChangeRequired) {
    }

}
