package com.chaula.dental.service;

import com.chaula.dental.domain.Patient;
import com.chaula.dental.repository.PatientRepository;
import com.chaula.dental.util.PhoneUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class PatientIdentityLookup {
    private PatientIdentityLookup() {
    }

    public static Optional<Patient> findMatching(
            PatientRepository patients,
            String normalizedPhone,
            String fullName,
            LocalDate dateOfBirth
    ) {
        String suffix = normalizedPhone.substring(normalizedPhone.length() - 10);
        List<Patient> candidates = new ArrayList<>(patients.findByPhoneNumberEndingWith(suffix));
        patients.findByPhoneNumberRegex(phoneSuffixRegex(suffix)).stream()
                .filter(existing -> candidates.stream().noneMatch(candidate ->
                        Objects.equals(existing.getId(), candidate.getId())))
                .forEach(candidates::add);
        patients.findByPhoneNumber(normalizedPhone)
                .filter(existing -> candidates.stream().noneMatch(candidate ->
                        Objects.equals(existing.getId(), candidate.getId())))
                .ifPresent(candidates::add);
        return candidates.stream()
                .filter(existing -> phoneNumberMatches(existing.getPhoneNumber(), normalizedPhone))
                .filter(existing -> existing.getFullName() != null
                        && existing.getFullName().strip().equalsIgnoreCase(fullName.strip()))
                .filter(existing -> dateOfBirth.equals(existing.getDateOfBirth()))
                .findFirst();
    }

    private static boolean phoneNumberMatches(String storedPhone, String normalizedPhone) {
        try {
            return PhoneUtils.normalizeIndianPhone(storedPhone).equals(normalizedPhone);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static String phoneSuffixRegex(String digits) {
        StringBuilder regex = new StringBuilder(".*");
        for (int index = 0; index < digits.length(); index++) {
            if (index > 0) regex.append("[^0-9]*");
            regex.append(digits.charAt(index));
        }
        return regex.append("[^0-9]*$").toString();
    }
}
