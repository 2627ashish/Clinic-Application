package com.chaula.dental.service;

import com.chaula.dental.messaging.event.WhatsAppListRow;

import java.util.List;

public interface WhatsAppMessagePublisher {
    void publishText(String phoneNumber, String text);

    void publishList(String phoneNumber, String text, String title, String button, List<WhatsAppListRow> rows);

    void publishLocation(String phoneNumber, String name, String address, double latitude, double longitude);

    void publishDocument(String phoneNumber, String fileName, String caption, byte[] document);

    void publishImage(String phoneNumber, String caption, String fileName, String contentType, byte[] image);
}
