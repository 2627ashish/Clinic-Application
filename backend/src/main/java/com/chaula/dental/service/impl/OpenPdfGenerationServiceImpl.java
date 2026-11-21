package com.chaula.dental.service.impl;

import com.chaula.dental.domain.Appointment;
import com.chaula.dental.domain.Patient;
import com.chaula.dental.domain.TreatmentAndBill;
import com.chaula.dental.service.PdfGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpenPdfGenerationServiceImpl implements PdfGenerationService {
    private final PrescriptionPdfGenerator prescriptionPdfGenerator;
    private final OutpatientBillPdfGenerator outpatientBillPdfGenerator;

    @Override
    public byte[] generateVisitSummaryPdf(Patient patient, TreatmentAndBill treatment) {
        return generatePrescriptionPdf(patient, treatment);
    }

    @Override
    public byte[] generatePrescriptionPdf(Patient patient, TreatmentAndBill treatment) {
        return prescriptionPdfGenerator.generate(patient, treatment);
    }

    @Override
    public byte[] generateOutpatientBillPdf(
            Patient patient,
            TreatmentAndBill treatment,
            Appointment appointment
    ) {
        return outpatientBillPdfGenerator.generate(patient, treatment, appointment);
    }
}
