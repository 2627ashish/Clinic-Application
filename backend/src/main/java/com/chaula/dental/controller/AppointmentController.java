package com.chaula.dental.controller;

import com.chaula.dental.domain.Appointment;
import com.chaula.dental.domain.Patient;
import com.chaula.dental.domain.PatientDocument;
import com.chaula.dental.domain.TreatmentAndBill;
import com.chaula.dental.dto.AiIntakeRequest;
import com.chaula.dental.dto.AppointmentStatusUpdateRequest;
import com.chaula.dental.dto.AppointmentResponse;
import com.chaula.dental.dto.ConsultationCompleteRequest;
import com.chaula.dental.dto.WalkInRequest;
import com.chaula.dental.dto.PatientDocumentView;
import com.chaula.dental.domain.PatientDocumentCategory;
import org.springframework.data.domain.Page;
import com.chaula.dental.service.PatientDocumentService;
import com.chaula.dental.service.AppointmentIntakeService;
import com.chaula.dental.service.AppointmentService;
import com.chaula.dental.service.PatientPortalService;
import com.chaula.dental.service.PdfGenerationService;
import com.chaula.dental.repository.AppointmentRepository;
import com.chaula.dental.repository.PatientRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {
    private final AppointmentService appointmentService;
    private final AppointmentIntakeService appointmentIntakeService;
    private final PatientDocumentService patientDocumentService;
    private final PatientPortalService patientPortalService;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final PdfGenerationService pdfGenerationService;

    @PostMapping("/ai-intake")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse createIntake(@Valid @RequestBody AiIntakeRequest request) {
        return appointmentIntakeService.processAiIntake(request);
    }

    @GetMapping("/pending")
    public List<AppointmentResponse> getPendingAppointments() {
        return appointmentIntakeService.findPendingAppointments();
    }

    @GetMapping
    public Page<AppointmentResponse> getAppointments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type
    ) {
        return appointmentService.getAppointments(page, size, status, type);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAppointment(@PathVariable String id) {
        appointmentService.deleteAppointment(id);
    }

    @GetMapping("/{id}/history")
    public List<TreatmentAndBill> getPatientHistory(@PathVariable String id) {
        return appointmentService.history(id);
    }

    @GetMapping("/{id}/bill-pdf")
    public ResponseEntity<byte[]> outpatientBillPdf(
            @PathVariable String id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        TreatmentAndBill visit = patientPortalService.visit(bearerToken(authorization), id);
        Patient patient = patientFor(visit);
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Appointment not found"));
        return pdfResponse(pdfGenerationService.generateOutpatientBillPdf(patient, visit, appointment),
                "outpatient-bill-" + id + ".pdf");
    }

    @GetMapping("/{id}/prescription-pdf")
    public ResponseEntity<byte[]> prescriptionPdf(
            @PathVariable String id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        TreatmentAndBill visit = patientPortalService.visit(bearerToken(authorization), id);
        Patient patient = patientFor(visit);
        return pdfResponse(pdfGenerationService.generatePrescriptionPdf(patient, visit),
                "prescription-" + id + ".pdf");
    }

    @GetMapping("/{id}/handwritten-rx/download")
    public ResponseEntity<byte[]> downloadHandwrittenRx(
            @PathVariable String id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        TreatmentAndBill visit = patientPortalService.visit(bearerToken(authorization), id);
        PatientDocument document = patientDocumentService.findHandwrittenForPatient(id, visit.getPatientId());
        MediaType contentType = MediaType.parseMediaType(document.getContentType());
        String extension = MediaType.IMAGE_JPEG.isCompatibleWith(contentType) ? "jpg" : "png";
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"Chauhan_Dental_Handwritten_Rx." + extension + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(document.getContent());
    }

    @PatchMapping("/{id}/status")
    public Appointment updateStatus(
            @PathVariable String id,
            @Valid @RequestBody AppointmentStatusUpdateRequest request
    ) {
        return appointmentService.updateStatus(id, request);
    }

    @PostMapping("/{id}/complete-visit")
    @ResponseStatus(HttpStatus.CREATED)
    public TreatmentAndBill completeVisit(
            @PathVariable String id,
            @Valid @RequestBody ConsultationCompleteRequest request
    ) {
        return appointmentService.completeVisit(id, request);
    }

    @PostMapping("/walk-ins")
    @ResponseStatus(HttpStatus.CREATED)
    public Appointment createWalkIn(@Valid @RequestBody WalkInRequest request) {
        return appointmentService.createWalkIn(request);
    }

    @PostMapping("/{id}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public PatientDocumentView uploadDocument(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") PatientDocumentCategory category
    ) {
        if (category == PatientDocumentCategory.HANDWRITTEN_RX) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Use the handwritten-rx upload endpoint");
        }
        return patientDocumentService.upload(id, file, category);
    }

    @PostMapping("/{id}/handwritten-rx")
    @ResponseStatus(HttpStatus.CREATED)
    public PatientDocumentView uploadHandwrittenPrescription(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file
    ) {
        return patientDocumentService.upload(id, file, PatientDocumentCategory.HANDWRITTEN_RX);
    }

    private Patient patientFor(TreatmentAndBill visit) {
        return patientRepository.findById(visit.getPatientId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Patient not found"));
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String fileName) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(pdf);
    }

    private String bearerToken(String authorization) {
        return authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7) : "";
    }
}
