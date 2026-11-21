package com.chaula.dental.service.impl;

import com.chaula.dental.domain.BillItem;
import com.chaula.dental.domain.Appointment;
import com.chaula.dental.domain.ClinicLocation;
import com.chaula.dental.domain.Patient;
import com.chaula.dental.domain.PrescriptionItem;
import com.chaula.dental.domain.TreatmentAndBill;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDate;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OpenPdfGenerationServiceImplTest {
    @Test
    void generatesTwoPagePrescriptionAndAftercareWithUnicodeAndClinicDetails() throws Exception {
        Patient patient = Patient.builder()
                .fullName("Asha Sharma")
                .phoneNumber("919810764715")
                .dateOfBirth(LocalDate.now().minusYears(32).minusDays(1))
                .gender("Male")
                .medicalHistory(List.of("Diabetes / Sugar", "Blood Thinners"))
                .ongoingTreatments("Cardiac follow-up")
                .build();
        TreatmentAndBill visit = TreatmentAndBill.builder()
                .symptomsAndDiagnosis("Tooth pain")
                .procedurePerformed("Root Canal Treatment (RCT)")
                .medicines(List.of(new PrescriptionItem("Amoxicillin", "1-0-1", "5 days", "After food")))
                .aftercareGuidelines("Soft diet only / केवल नरम आहार लें")
                .billItems(List.of(new BillItem("Filling", new BigDecimal("800"))))
                .totalAmount(new BigDecimal("800"))
                .paidAmount(new BigDecimal("500"))
                .build();

        byte[] pdf = generator().generateVisitSummaryPdf(patient, visit);

        assertThat(pdf).startsWith("%PDF".getBytes());
        PdfReader reader = new PdfReader(pdf);
        assertThat(reader.getNumberOfPages()).isEqualTo(2);
        String text = new PdfTextExtractor(reader).getTextFromPage(1);
        assertThat(text).contains("CHAUHAN DENTAL CLINIC", "DR. B.S. CHAUHAN", "डॉ. बी. एस. चौहान", "A-5651",
                "Age / Sex: 32 years / Male", "RCT", "Amoxicillin", "5 days",
                "Gali No. 3, Chhalera", "Madhuban Apartments", "Rate our clinic on JustDial");
        String aftercare = new PdfTextExtractor(reader).getTextFromPage(2);
        assertThat(aftercare).contains("POST-TREATMENT CARE & PRECAUTIONS", "Root Canal Care",
                "RECOMMENDED SOFT DIET", "+91-98107-64715", "permanent crown");
        reader.close();
    }

    @Test
    void generatesItemizedOutpatientBillAndExplicitlyMarksMissingUpiQrConfiguration() throws Exception {
        Patient patient = Patient.builder()
                .id("01234567890123456789abcde")
                .fullName("Asha Sharma")
                .phoneNumber("919810764715")
                .dateOfBirth(LocalDate.now().minusYears(32))
                .gender("Female")
                .build();
        TreatmentAndBill visit = TreatmentAndBill.builder()
                .appointmentId("appointment-1")
                .createdAt(Instant.parse("2026-11-20T08:30:00Z"))
                .billItems(List.of(
                        new BillItem("CONSULTATION - DR. B.S. CHAUHAN", new BigDecimal("500.00")),
                        new BillItem("ROOT CANAL TREATMENT (RCT)", new BigDecimal("2500.00"))))
                .totalAmount(new BigDecimal("3000.00"))
                .paidAmount(new BigDecimal("500.00"))
                .build();
        Appointment appointment = Appointment.builder().clinicLocation(ClinicLocation.SEC_82).build();

        byte[] pdf = generator().generateOutpatientBillPdf(patient, visit, appointment);

        PdfReader reader = new PdfReader(pdf);
        assertThat(reader.getNumberOfPages()).isEqualTo(1);
        String text = new PdfTextExtractor(reader).getTextFromPage(1);
        assertThat(text).contains("OUT PATIENT BILL", "CDC-ABCDE", "999312",
                "CONSULTATION - DR. B.S. CHAUHAN", "ROOT CANAL TREATMENT (RCT)",
                "Net Patient Payable", "₹2500.00", "Rupees Three Thousand Only",
                "Partially Paid", "UPI SCAN & PAY", "QR not configured");
        reader.close();
    }

    private OpenPdfGenerationServiceImpl generator() {
        ClinicPdfResources resources = new ClinicPdfResources();
        return new OpenPdfGenerationServiceImpl(
                new PrescriptionPdfGenerator(resources, new PrescriptionAftercarePage(resources)),
                new OutpatientBillPdfGenerator(resources));
    }
}
