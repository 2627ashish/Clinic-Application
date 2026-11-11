package com.chaula.dental.controller;

import com.chaula.dental.domain.Patient;
import com.chaula.dental.domain.TreatmentAndBill;
import com.chaula.dental.dto.PatientOtpRequest;
import com.chaula.dental.dto.PatientOtpResponse;
import com.chaula.dental.dto.PatientOtpVerifyRequest;
import com.chaula.dental.dto.PatientPortalSessionResponse;
import com.chaula.dental.dto.PatientVisitSummary;
import com.chaula.dental.repository.PatientRepository;
import com.chaula.dental.service.PatientPortalService;
import com.chaula.dental.service.PdfGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/patient-portal")
@RequiredArgsConstructor
public class PatientPortalController {
    private final PatientPortalService portalService;
    private final PatientRepository patientRepository;
    private final PdfGenerationService pdfService;

    @PostMapping("/otp")
    public PatientOtpResponse sendOtp(@Valid @RequestBody PatientOtpRequest request) {
        return portalService.sendOtp(request.phoneNumber());
    }

    @PostMapping("/verify")
    public PatientPortalSessionResponse verifyOtp(@Valid @RequestBody PatientOtpVerifyRequest request) {
        return portalService.verifyOtp(request.phoneNumber(), request.code());
    }

    @GetMapping("/visits")
    public List<PatientVisitSummary> visits(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return portalService.visits(bearerToken(authorization));
    }

    @GetMapping("/visits/{appointmentId}/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable String appointmentId
    ) {
        TreatmentAndBill visit = portalService.visit(bearerToken(authorization), appointmentId);
        Patient patient = patientRepository.findById(visit.getPatientId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Patient not found"));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"visit-summary.pdf\"")
                .body(pdfService.generateVisitSummaryPdf(patient, visit));
    }

    private String bearerToken(String authorization) {
        return authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7) : "";
    }
}
