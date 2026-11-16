package com.chaula.dental.service;

import com.chaula.dental.domain.Appointment;
import com.chaula.dental.domain.PatientDocument;
import com.chaula.dental.domain.PatientDocumentCategory;
import com.chaula.dental.dto.PatientDocumentView;
import com.chaula.dental.repository.AppointmentRepository;
import com.chaula.dental.repository.PatientDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PatientDocumentService {
    private static final long MAX_FILE_SIZE = 8L * 1024 * 1024;
    private final AppointmentRepository appointments;
    private final PatientDocumentRepository documents;

    public PatientDocumentView upload(
            String appointmentId,
            MultipartFile file,
            PatientDocumentCategory category
    ) {
        Appointment appointment = appointments.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a non-empty document to upload");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Documents must be 8 MB or smaller");
        }
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equalsIgnoreCase("application/pdf")
                || contentType.equalsIgnoreCase("image/jpeg")
                || contentType.equalsIgnoreCase("image/png")
                || contentType.equalsIgnoreCase("image/webp"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload a PDF, JPEG, PNG, or WebP file");
        }
        try {
            String originalName = file.getOriginalFilename();
            String safeOriginalName = originalName == null ? "" : originalName.replace('\\', '/');
            String fileName = safeOriginalName.isBlank()
                    ? "patient-document" : Paths.get(safeOriginalName).getFileName().toString();
            fileName = fileName.replaceAll("[\\p{Cntrl}\"]", "_");
            if (fileName.length() > 200) fileName = fileName.substring(fileName.length() - 200);
            PatientDocument saved = documents.save(PatientDocument.builder()
                    .patientId(appointment.getPatientId())
                    .appointmentId(appointment.getId())
                    .fileName(fileName)
                    .contentType(contentType.toLowerCase(Locale.ROOT))
                    .category(category)
                    .content(file.getBytes())
                    .build());
            return PatientDocumentView.from(saved);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded document could not be read", exception);
        }
    }

    public List<PatientDocumentView> listForPatient(String patientId) {
        return documents.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(PatientDocumentView::from)
                .toList();
    }

    public PatientDocument findForPatient(String documentId, String patientId) {
        return documents.findByIdAndPatientId(documentId, patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }
}
