package com.chaula.dental.service;

import com.chaula.dental.domain.Patient;
import com.chaula.dental.dto.BroadcastRequest;
import com.chaula.dental.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class CampaignService {
    private final PatientRepository patientRepository;
    private final WhatsAppNotificationService whatsappService;

    public void broadcast(BroadcastRequest request, MultipartFile image) {
        byte[] imageBytes = readImage(image);
        String contentType = image == null || image.isEmpty() ? null : image.getContentType();
        String fileName = image == null || image.isEmpty() ? null : image.getOriginalFilename();
        for (Patient patient : patientRepository.findByWhatsappMarketingOptInTrue()) {
            if (imageBytes == null) {
                whatsappService.sendTextMessageAsync(patient.getPhoneNumber(), request.message());
            } else {
                whatsappService.sendImageAsync(
                        patient.getPhoneNumber(), request.message(), fileName, contentType, imageBytes);
            }
        }
    }

    public long eligiblePatientCount() {
        return patientRepository.countByWhatsappMarketingOptInTrue();
    }

    private byte[] readImage(MultipartFile image) {
        if (image == null || image.isEmpty()) return null;
        if (image.getSize() > 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campaign images must be 1 MB or smaller.");
        }
        String contentType = image.getContentType();
        if (!"image/jpeg".equals(contentType) && !"image/png".equals(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPEG and PNG images are supported.");
        }
        try {
            byte[] bytes = image.getBytes();
            if (!hasValidImageSignature(bytes, contentType)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file is not a valid JPEG or PNG image.");
            }
            return bytes;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read the campaign image.", exception);
        }
    }

    private boolean hasValidImageSignature(byte[] bytes, String contentType) {
        if ("image/png".equals(contentType)) {
            return bytes.length >= 8
                    && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50
                    && bytes[2] == 0x4e && bytes[3] == 0x47
                    && bytes[4] == 0x0d && bytes[5] == 0x0a
                    && bytes[6] == 0x1a && bytes[7] == 0x0a;
        }
        return bytes.length >= 3 && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff;
    }
}
