package com.chaula.dental.service;

import com.chaula.dental.repository.AppointmentRepository;
import com.chaula.dental.repository.PatientDocumentRepository;
import com.chaula.dental.repository.PatientRepository;
import com.chaula.dental.repository.TreatmentAndBillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PatientService {
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientDocumentRepository patientDocumentRepository;
    private final TreatmentAndBillRepository treatmentRepository;

    public void deletePatient(String id) {
        if (!patientRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
        }
        if (!appointmentRepository.findByPatientId(id).isEmpty()
                || !patientDocumentRepository.findByPatientId(id).isEmpty()
                || !treatmentRepository.findByPatientId(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This patient has appointment or clinical records and cannot be deleted.");
        }
        patientRepository.deleteById(id);
    }
}
