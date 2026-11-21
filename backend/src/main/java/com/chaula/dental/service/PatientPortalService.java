package com.chaula.dental.service;

import com.chaula.dental.domain.Patient;
import com.chaula.dental.domain.TreatmentAndBill;
import com.chaula.dental.dto.AuthResponse;
import com.chaula.dental.dto.PatientProfileView;
import com.chaula.dental.dto.PatientVisitSummary;
import com.chaula.dental.repository.PatientRepository;
import com.chaula.dental.repository.TreatmentAndBillRepository;
import com.chaula.dental.util.PhoneUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PatientPortalService {
    private static final long SESSION_SECONDS = 12 * 60 * 60;
    private final Map<String, PatientSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> activeSessionsByPatient = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final PatientRepository patients;
    private final TreatmentAndBillRepository treatments;

    public PatientPortalService(
            PatientRepository patients,
            TreatmentAndBillRepository treatments
    ) {
        this.patients = patients;
        this.treatments = treatments;
    }

    public AuthResponse createSessionsForPhone(String normalizedPhone) {
        String tenDigits = normalizedPhone.substring(normalizedPhone.length() - 10);
        List<Patient> candidates = new ArrayList<>(patients.findAllByPhoneNumber(normalizedPhone));
        candidates.addAll(patients.findByPhoneNumberEndingWith(tenDigits));
        candidates.addAll(patients.findByPhoneNumberRegex(phoneSuffixRegex(tenDigits)));
        Map<String, Patient> matchedPatients = new LinkedHashMap<>();
        candidates.stream()
                .filter(patient -> phoneNumberMatches(patient.getPhoneNumber(), normalizedPhone))
                .forEach(patient -> matchedPatients.putIfAbsent(patient.getId(), patient));
        if (matchedPatients.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No patient account was found for this number");
        }

        List<AuthResponse.PatientSession> patientSessions = matchedPatients.values().stream()
                .map(this::createSession)
                .toList();
        return new AuthResponse(patientSessions);
    }

    private AuthResponse.PatientSession createSession(Patient patient) {
        String token = newToken();
        activeSessionsByPatient.compute(patient.getId(), (patientId, previousToken) -> {
            if (previousToken != null) sessions.remove(previousToken);
            sessions.put(token, new PatientSession(patientId, Instant.now().plusSeconds(SESSION_SECONDS)));
            return token;
        });
        return new AuthResponse.PatientSession(token, patient.getFullName());
    }

    private boolean phoneNumberMatches(String storedPhone, String normalizedPhone) {
        try {
            return PhoneUtils.normalizeIndianPhone(storedPhone).equals(normalizedPhone);
        } catch (IllegalArgumentException exception) {
            return false;
        }
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

    public PatientProfileView profile(String token) {
        Patient patient = authenticatedPatient(token);
        return new PatientProfileView(
                patient.getFullName(),
                patient.getPhoneNumber(),
                patient.getAge(),
                patient.getDateOfBirth(),
                patient.getGender()
        );
    }

    public String patientId(String token) {
        return authenticatedPatient(token).getId();
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
                visit.getProcedurePerformed(),
                visit.getMedicines() == null ? List.of() : visit.getMedicines(),
                visit.getAftercareGuidelines()
        );
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record PatientSession(String patientId, Instant expiresAt) {
    }
}
