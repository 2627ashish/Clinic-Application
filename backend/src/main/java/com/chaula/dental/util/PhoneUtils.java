package com.chaula.dental.util;

public final class PhoneUtils {
    private PhoneUtils() {
    }

    public static String normalizeIndianPhone(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Phone number is required");
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 10 && digits.matches("[6-9]\\d{9}")) {
            return "91" + digits;
        }
        if (digits.length() == 11 && digits.startsWith("0")
                && digits.substring(1).matches("[6-9]\\d{9}")) {
            return "91" + digits.substring(1);
        }
        if (digits.length() == 12 && digits.startsWith("91")
                && digits.substring(2).matches("[6-9]\\d{9}")) {
            return digits;
        }
        throw new IllegalArgumentException("Enter a valid Indian mobile number");
    }
}
