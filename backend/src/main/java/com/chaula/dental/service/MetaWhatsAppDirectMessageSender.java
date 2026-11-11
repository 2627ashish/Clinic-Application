package com.chaula.dental.service;

import com.chaula.dental.messaging.event.WhatsAppDocumentMessage;
import com.chaula.dental.messaging.event.WhatsAppImageMessage;
import com.chaula.dental.messaging.event.WhatsAppLocationMessage;
import com.chaula.dental.messaging.event.WhatsAppOutboundMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MetaWhatsAppDirectMessageSender implements WhatsAppDirectMessageSender {
    private final MetaWhatsAppCloudApiClient cloudApiClient;

    @Override
    public void send(Object message) {
        if (message instanceof WhatsAppOutboundMessage outboundMessage) {
            cloudApiClient.send(outboundMessage);
        } else if (message instanceof WhatsAppDocumentMessage documentMessage) {
            cloudApiClient.sendPdfDocument(
                    documentMessage.phoneNumber(),
                    documentMessage.document(),
                    documentMessage.fileName(),
                    documentMessage.caption()
            );
        } else if (message instanceof WhatsAppImageMessage imageMessage) {
            cloudApiClient.sendImage(imageMessage);
        } else if (message instanceof WhatsAppLocationMessage locationMessage) {
            cloudApiClient.sendLocation(locationMessage);
        } else {
            throw new IllegalArgumentException("Unsupported WhatsApp outbound message type: "
                    + message.getClass().getName());
        }
    }
}
