package com.chaula.dental.service;

import java.util.Locale;

final class PatientDemographics {
    private PatientDemographics() {
    }

    static Details parse(String value) {
        String[] parts = value.split(",", -1);
        if (parts.length != 3) return null;
        String name = parts[0].replaceAll("\\s+", " ").trim();
        int age;
        try {
            age = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException exception) {
            return null;
        }
        String gender = switch (parts[2].trim().toLowerCase(Locale.ROOT)) {
            case "male", "m" -> "Male";
            case "female", "f" -> "Female";
            case "other" -> "Other";
            default -> "";
        };
        if (name.isBlank() || age < 1 || age > 120 || gender.isBlank()) return null;
        return new Details(name, age, gender);
    }

    record Details(String name, int age, String gender) {
    }
}
