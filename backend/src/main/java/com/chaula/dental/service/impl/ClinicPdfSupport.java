package com.chaula.dental.service.impl;

import com.chaula.dental.domain.BillItem;
import com.chaula.dental.domain.Patient;
import com.chaula.dental.domain.TreatmentAndBill;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

final class ClinicPdfSupport {
    static final Color BRAND = new Color(21, 94, 82);
    static final Color PALE = new Color(242, 247, 246);
    static final Color PURPLE = new Color(108, 80, 153);
    static final Color GREEN = new Color(31, 126, 85);
    static final Color RED = new Color(170, 45, 54);
    private static final BigInteger HUNDRED = BigInteger.valueOf(100);
    private static final BigInteger THOUSAND = BigInteger.valueOf(1_000);
    private static final BigInteger LAKH = BigInteger.valueOf(100_000);
    private static final BigInteger CRORE = BigInteger.valueOf(10_000_000);
    private static final String[] ONES = {
            "Zero", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen",
            "Eighteen", "Nineteen"
    };
    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private ClinicPdfSupport() {
    }

    static String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    static String money(BigDecimal value) {
        return "₹" + amount(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    static BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    static List<BillItem> billItems(TreatmentAndBill treatment) {
        return treatment.getBillItems() == null ? List.of() : treatment.getBillItems();
    }

    static String patientNumber(Patient patient) {
        String id = patient.getId();
        if (id == null || id.isBlank()) return "Not assigned";
        String suffix = id.substring(Math.max(0, id.length() - 5)).toUpperCase(Locale.ROOT);
        return "CDC-" + suffix;
    }

    static String ageAndSex(Patient patient) {
        Integer age = patient.getAge();
        String gender = patient.getGender();
        String sex = gender == null || gender.isBlank() ? "Not recorded" : gender;
        return (age == null ? "Age not recorded" : age + " years") + " / " + sex;
    }

    static LocalDate billDate(TreatmentAndBill treatment) {
        Instant createdAt = treatment.getCreatedAt();
        return createdAt == null
                ? LocalDate.now()
                : createdAt.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();
    }

    static PdfPCell textCell(String text, Font font, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setBorderColor(new Color(220, 226, 226));
        if (background != null) cell.setBackgroundColor(background);
        return cell;
    }

    static PdfPCell headerCell(String text, Font font, Color background) {
        PdfPCell cell = textCell(text, font, background);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    static Paragraph section(String title, Font font) {
        Paragraph paragraph = new Paragraph(title, font);
        paragraph.setSpacingBefore(7);
        paragraph.setSpacingAfter(4);
        return paragraph;
    }

    static String rupeesInWords(BigDecimal value) {
        BigDecimal normalized = amount(value).setScale(2, RoundingMode.HALF_UP);
        BigInteger rupees = normalized.toBigInteger();
        int paise = normalized.remainder(BigDecimal.ONE).movePointRight(2).intValue();
        String words = "Rupees " + numberWords(rupees);
        if (paise > 0) words += " and " + numberWords(BigInteger.valueOf(paise)) + " Paise";
        return words + " Only";
    }

    private static String numberWords(BigInteger value) {
        if (value.compareTo(BigInteger.valueOf(20)) < 0) return ONES[value.intValue()];
        if (value.compareTo(BigInteger.valueOf(100)) < 0) {
            return TENS[value.divide(BigInteger.TEN).intValue()]
                    + (value.remainder(BigInteger.TEN).signum() == 0 ? "" : " " + numberWords(value.remainder(BigInteger.TEN)));
        }
        if (value.compareTo(THOUSAND) < 0) {
            return numberWords(value.divide(HUNDRED)) + " Hundred" + remainderWords(value, HUNDRED);
        }
        if (value.compareTo(LAKH) < 0) {
            return numberWords(value.divide(THOUSAND)) + " Thousand" + remainderWords(value, THOUSAND);
        }
        if (value.compareTo(CRORE) < 0) {
            return numberWords(value.divide(LAKH)) + " Lakh" + remainderWords(value, LAKH);
        }
        return numberWords(value.divide(CRORE)) + " Crore" + remainderWords(value, CRORE);
    }

    private static String remainderWords(BigInteger value, BigInteger divisor) {
        BigInteger remainder = value.remainder(divisor);
        return remainder.signum() == 0 ? "" : " " + numberWords(remainder);
    }
}
