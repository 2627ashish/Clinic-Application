package com.chaula.dental.service.impl;

import com.chaula.dental.domain.ClinicLocation;
import com.chaula.dental.service.WhatsAppMessagePublisher;
import com.chaula.dental.service.WhatsAppNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaWhatsAppNotificationService implements WhatsAppNotificationService {
    private final WhatsAppMessagePublisher messagePublisher;

    @Override
    public void sendTextMessageAsync(String phoneNumber, String message) {
        messagePublisher.publishText(phoneNumber, message);
    }

    @Override
    public void sendLocationMessageAsync(String phoneNumber, ClinicLocation location) {
        ClinicLocation clinic = ClinicLocation.defaultLocation(location);
        messagePublisher.publishLocation(
                phoneNumber, clinic.label(), clinic.address(), clinic.latitude(), clinic.longitude());
    }

    @Override
    @Async("whatsappExecutor")
    public void sendDocumentAsync(String phoneNumber, String fileName, byte[] document) {
        messagePublisher.publishDocument(
                phoneNumber,
                fileName,
                "Your visit summary and prescription from Dr. B.S. Chauhan. "
                        + "For clinic directions, reply LOCATION. "
                        + "+91-98107-64715 / +91-97177-73179",
                document
        );
    }

    @Override
    public void sendImageAsync(String phoneNumber, String caption, String fileName, String contentType, byte[] image) {
        messagePublisher.publishImage(phoneNumber, caption, fileName, contentType, image);
    }
}
