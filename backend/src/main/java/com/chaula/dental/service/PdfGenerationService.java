package com.chaula.dental.service;

import com.chaula.dental.domain.Patient;
import com.chaula.dental.domain.TreatmentAndBill;

public interface PdfGenerationService {
    byte[] generateVisitSummaryPdf(Patient patient, TreatmentAndBill treatment);
}
