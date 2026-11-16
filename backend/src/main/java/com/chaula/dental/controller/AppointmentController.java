package com.chaula.dental.controller;

import com.chaula.dental.domain.Appointment;
import com.chaula.dental.domain.TreatmentAndBill;
import com.chaula.dental.dto.AiIntakeRequest;
import com.chaula.dental.dto.AppointmentStatusUpdateRequest;
import com.chaula.dental.dto.AppointmentResponse;
import com.chaula.dental.dto.AppointmentView;
import com.chaula.dental.dto.ConsultationCompleteRequest;
import com.chaula.dental.dto.WalkInRequest;
import com.chaula.dental.dto.PatientDocumentView;
import com.chaula.dental.domain.PatientDocumentCategory;
import com.chaula.dental.service.PatientDocumentService;
import com.chaula.dental.service.AppointmentIntakeService;
import com.chaula.dental.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {
    private final AppointmentService appointmentService;
    private final AppointmentIntakeService appointmentIntakeService;
    private final PatientDocumentService patientDocumentService;

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
    public List<AppointmentView> getAppointments() {
        return appointmentService.getAppointments();
    }

    @GetMapping("/{id}/history")
    public List<TreatmentAndBill> getPatientHistory(@PathVariable String id) {
        return appointmentService.history(id);
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
}
