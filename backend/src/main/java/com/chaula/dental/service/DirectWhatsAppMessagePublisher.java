package com.chaula.dental.service;

import com.chaula.dental.messaging.event.WhatsAppDocumentMessage;
import com.chaula.dental.messaging.event.WhatsAppImageMessage;
import com.chaula.dental.messaging.event.WhatsAppListRow;
import com.chaula.dental.messaging.event.WhatsAppLocationMessage;
import com.chaula.dental.messaging.event.WhatsAppOutboundMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DirectWhatsAppMessagePublisher implements WhatsAppMessagePublisher {
    private final WhatsAppDirectMessageSender directSender;

    @Override
    @Async("whatsappExecutor")
    public void publish(WhatsAppOutboundMessage message) {
        directSender.send(message);
    }

    @Override
    @Async("whatsappExecutor")
    public void publishText(String phoneNumber, String text) {
        directSender.send(WhatsAppOutboundMessage.text(phoneNumber, text));
    }

    @Override
    @Async("whatsappExecutor")
    public void publishList(String phoneNumber, String text, String title, String button, List<WhatsAppListRow> rows) {
        directSender.send(WhatsAppOutboundMessage.list(phoneNumber, text, title, button, rows));
    }

    @Override
    @Async("whatsappExecutor")
    public void publishLocation(String phoneNumber, String name, String address, double latitude, double longitude) {
        directSender.send(new WhatsAppLocationMessage(phoneNumber, name, address, latitude, longitude));
    }

    @Override
    @Async("whatsappExecutor")
    public void publishDocument(String phoneNumber, String fileName, String caption, byte[] document) {
        directSender.send(new WhatsAppDocumentMessage(phoneNumber, fileName, caption, document));
    }

    @Override
    @Async("whatsappExecutor")
    public void publishImage(String phoneNumber, String caption, String fileName, String contentType, byte[] image) {
        directSender.send(new WhatsAppImageMessage(phoneNumber, caption, contentType, fileName, image));
    }
}
