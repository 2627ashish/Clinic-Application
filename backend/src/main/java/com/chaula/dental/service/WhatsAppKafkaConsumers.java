package com.chaula.dental.service;

import com.chaula.dental.messaging.event.WhatsAppDocumentMessage;
import com.chaula.dental.messaging.event.WhatsAppOutboundMessage;
import com.chaula.dental.messaging.event.WhatsAppImageMessage;
import com.chaula.dental.messaging.event.WhatsAppLocationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WhatsAppKafkaConsumers {
    private final MetaWhatsAppCloudApiClient cloudApiClient;

    @KafkaListener(
            topics = "${whatsapp.kafka.outbound-topic:whatsapp-outbound}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void sendOutbound(WhatsAppOutboundMessage message) {
        cloudApiClient.send(message);
    }

    @KafkaListener(
            topics = "${whatsapp.kafka.document-topic:whatsapp-outbound-documents}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void sendDocument(WhatsAppDocumentMessage message) {
        cloudApiClient.sendPdfDocument(
                message.phoneNumber(), message.document(), message.fileName(), message.caption());
    }

    @KafkaListener(
            topics = "${whatsapp.kafka.image-topic:whatsapp-outbound-images}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void sendImage(WhatsAppImageMessage message) {
        cloudApiClient.sendImage(message);
    }

    @KafkaListener(
            topics = "${whatsapp.kafka.location-topic:whatsapp-outbound-locations}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void sendLocation(WhatsAppLocationMessage message) {
        cloudApiClient.sendLocation(message);
    }
}
