package com.chaula.dental.service.impl;

import com.chaula.dental.domain.BillItem;
import com.chaula.dental.domain.Patient;
import com.chaula.dental.domain.PrescriptionItem;
import com.chaula.dental.domain.TreatmentAndBill;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenPdfGenerationServiceImplTest {
    @Test
    void generatesBrandedVisitSummaryWithUnicodeAndClinicDetails() throws Exception {
        Patient patient = Patient.builder()
                .fullName("Asha Sharma")
                .phoneNumber("919810764715")
                .age(32)
                .gender("Male")
                .build();
        TreatmentAndBill visit = TreatmentAndBill.builder()
                .symptomsAndDiagnosis("Tooth pain")
                .procedurePerformed("Dental filling")
                .medicines(List.of(new PrescriptionItem("Amoxicillin", "1-0-1", "5 days", "After food")))
                .aftercareGuidelines("Soft diet only / केवल नरम आहार लें")
                .billItems(List.of(new BillItem("Filling", new BigDecimal("800"))))
                .totalAmount(new BigDecimal("800"))
                .paidAmount(new BigDecimal("500"))
                .build();

        byte[] pdf = new OpenPdfGenerationServiceImpl().generateVisitSummaryPdf(patient, visit);

        assertThat(pdf).startsWith("%PDF".getBytes());
        PdfReader reader = new PdfReader(pdf);
        assertThat(reader.getNumberOfPages()).isEqualTo(1);
        String text = new PdfTextExtractor(reader).getTextFromPage(1);
        assertThat(text).contains("CHAUHAN DENTAL CLINIC", "DR. B.S. CHAUHAN", "डॉ. बी. एस. चौहान", "A-5651",
                "Age/Sex: 32/M", "Root Canal Treatment", "Amoxicillin", "5 days", "Gali No. 3, Chhalera",
                "Madhuban Apartments", "Clinic 1 map directions", "Clinic 2 map directions", "केवल नरम आहार लें");
        reader.close();
    }
}
