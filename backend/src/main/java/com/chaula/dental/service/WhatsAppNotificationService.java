package com.chaula.dental.service;

import com.chaula.dental.domain.ClinicLocation;

public interface WhatsAppNotificationService {
    void sendTextMessageAsync(String phoneNumber, String message);

    void sendLocationMessageAsync(String phoneNumber, ClinicLocation location);

    void sendDocumentAsync(String phoneNumber, String fileName, byte[] document);

    void sendImageAsync(String phoneNumber, String caption, String fileName, String contentType, byte[] image);
}
