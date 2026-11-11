package com.chaula.dental.service;

import com.chaula.dental.domain.Patient;
import com.chaula.dental.domain.TreatmentAndBill;
import com.chaula.dental.dto.PatientOtpResponse;
import com.chaula.dental.dto.PatientPortalSessionResponse;
import com.chaula.dental.dto.PatientVisitSummary;
import com.chaula.dental.repository.PatientRepository;
import com.chaula.dental.repository.TreatmentAndBillRepository;
import com.chaula.dental.util.PhoneUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PatientPortalService {
    private static final long OTP_SECONDS = 10 * 60;
    private static final long SESSION_SECONDS = 12 * 60 * 60;
    private final Map<String, OtpEntry> pendingOtps = new ConcurrentHashMap<>();
    private final Map<String, PatientSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> activeSessionsByPatient = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final PatientRepository patients;
    private final TreatmentAndBillRepository treatments;
    private final WhatsAppMessagePublisher publisher;
    private final boolean mockOtp;

    public PatientPortalService(
            PatientRepository patients,
            TreatmentAndBillRepository treatments,
            WhatsAppMessagePublisher publisher,
            @Value("${patient.portal.mock-otp:false}") boolean mockOtp
    ) {
        this.patients = patients;
        this.treatments = treatments;
        this.publisher = publisher;
        this.mockOtp = mockOtp;
    }

    public PatientOtpResponse sendOtp(String rawPhone) {
        String phone = normalize(rawPhone);
        String code = String.format("%04d", random.nextInt(10_000));
        pendingOtps.put(phone, new OtpEntry(code, Instant.now().plusSeconds(OTP_SECONDS), new AtomicInteger()));
        publisher.publishText(phone, "Your Chauhan Dental Clinic login OTP is " + code + ". Valid for 10 minutes.");
        return new PatientOtpResponse("If this number is registered, an OTP has been sent on WhatsApp.",
                mockOtp ? code : null);
    }

    public PatientPortalSessionResponse verifyOtp(String rawPhone, String code) {
        String phone = normalize(rawPhone);
        OtpEntry entry = pendingOtps.get(phone);
        if (entry == null || !entry.expiresAt().isAfter(Instant.now())
                || entry.attempts().incrementAndGet() > 5 || !entry.code().equals(code)) {
            if (entry != null && entry.attempts().get() > 5) pendingOtps.remove(phone, entry);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The code is invalid or expired");
        }
        Patient patient = findPatientByPhone(phone)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "No patient account was found for this number"));
        pendingOtps.remove(phone, entry);
        String token = newToken();
        activeSessionsByPatient.compute(patient.getId(), (patientId, previousToken) -> {
            if (previousToken != null) sessions.remove(previousToken);
            sessions.put(token, new PatientSession(patientId, Instant.now().plusSeconds(SESSION_SECONDS)));
            return token;
        });
        return new PatientPortalSessionResponse(token, patient.getFullName());
    }

    private Optional<Patient> findPatientByPhone(String normalizedPhone) {
        String tenDigits = normalizedPhone.substring(normalizedPhone.length() - 10);
        Optional<Patient> exact = patients.findByPhoneNumber(normalizedPhone);
        if (exact.isPresent()) return exact;
        Optional<Patient> suffix = patients.findFirstByPhoneNumberEndingWith(tenDigits);
        if (suffix.isPresent()) return suffix;
        return patients.findFirstByPhoneNumberRegex(phoneSuffixRegex(tenDigits));
    }

    private String phoneSuffixRegex(String digits) {
        StringBuilder regex = new StringBuilder(".*");
        for (int index = 0; index < digits.length(); index++) {
            if (index > 0) regex.append("[^0-9]*");
            regex.append(digits.charAt(index));
        }
        return regex.append("[^0-9]*$").toString();
    }

    public List<PatientVisitSummary> visits(String token) {
        Patient patient = authenticatedPatient(token);
        return treatments.findByPatientIdOrderByCreatedAtDesc(patient.getId()).stream()
                .map(this::toVisitSummary)
                .toList();
    }

    public TreatmentAndBill visit(String token, String appointmentId) {
        Patient patient = authenticatedPatient(token);
        return treatments.findByAppointmentId(appointmentId)
                .filter(visit -> visit.getPatientId().equals(patient.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visit not found"));
    }

    private Patient authenticatedPatient(String token) {
        PatientSession session = token == null ? null : sessions.get(token);
        if (session == null || !session.expiresAt().isAfter(Instant.now())) {
            if (token != null) {
                PatientSession expired = sessions.remove(token);
                if (expired != null) activeSessionsByPatient.remove(expired.patientId(), token);
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Patient login is required");
        }
        return patients.findById(session.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Patient account no longer exists"));
    }

    private PatientVisitSummary toVisitSummary(TreatmentAndBill visit) {
        return new PatientVisitSummary(
                visit.getAppointmentId(),
                visit.getCreatedAt(),
                visit.getSymptomsAndDiagnosis(),
                visit.getProcedurePerformed()
        );
    }

    private String normalize(String phone) {
        try {
            return PhoneUtils.normalizeIndianPhone(phone);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record OtpEntry(String code, Instant expiresAt, AtomicInteger attempts) {
    }

    private record PatientSession(String patientId, Instant expiresAt) {
    }
}
