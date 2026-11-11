package com.chaula.dental.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "treatments_and_bills")
public class TreatmentAndBill {
    @Id
    private String id;

    private String appointmentId;
    private String patientId;
    private String symptomsAndDiagnosis;
    private String procedurePerformed;
    private List<PrescriptionItem> medicines;
    private String aftercareGuidelines;
    private List<BillItem> billItems;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
