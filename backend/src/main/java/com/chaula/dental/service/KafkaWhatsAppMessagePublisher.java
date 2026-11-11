package com.chaula.dental.service;

import com.chaula.dental.messaging.event.WhatsAppListRow;
import com.chaula.dental.messaging.event.WhatsAppOutboundMessage;
import com.chaula.dental.messaging.event.WhatsAppDocumentMessage;
import com.chaula.dental.messaging.event.WhatsAppImageMessage;
import com.chaula.dental.messaging.event.WhatsAppLocationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaWhatsAppMessagePublisher implements WhatsAppMessagePublisher {
    private final WhatsAppOutboundEventSender eventSender;
    private final WhatsAppDirectMessageSender directMessageSender;

    @Value("${whatsapp.kafka.outbound-topic:whatsapp-outbound}")
    private String outboundTopic;

    @Value("${whatsapp.kafka.document-topic:whatsapp-outbound-documents}")
    private String documentTopic;

    @Value("${whatsapp.kafka.image-topic:whatsapp-outbound-images}")
    private String imageTopic;

    @Value("${whatsapp.kafka.location-topic:whatsapp-outbound-locations}")
    private String locationTopic;

    @Override
    public void publishText(String phoneNumber, String text) {
        publish(WhatsAppOutboundMessage.text(phoneNumber, text));
    }

    @Override
    public void publishList(
            String phoneNumber,
            String text,
            String title,
            String button,
            List<WhatsAppListRow> rows
    ) {
        publish(WhatsAppOutboundMessage.list(phoneNumber, text, title, button, rows));
    }

    @Override
    public void publishLocation(String phoneNumber, String name, String address, double latitude, double longitude) {
        WhatsAppLocationMessage message =
                new WhatsAppLocationMessage(phoneNumber, name, address, latitude, longitude);
        publish(locationTopic, phoneNumber, message);
    }

    @Override
    public void publishDocument(String phoneNumber, String fileName, String caption, byte[] document) {
        WhatsAppDocumentMessage message = new WhatsAppDocumentMessage(phoneNumber, fileName, caption, document);
        publish(documentTopic, message.phoneNumber(), message);
    }

    @Override
    public void publishImage(String phoneNumber, String caption, String fileName, String contentType, byte[] image) {
        WhatsAppImageMessage message = new WhatsAppImageMessage(phoneNumber, caption, contentType, fileName, image);
        publish(imageTopic, message.phoneNumber(), message);
    }

    private void publish(WhatsAppOutboundMessage message) {
        publish(outboundTopic, message.phoneNumber(), message);
    }

    private void publish(String topic, String phoneNumber, Object message) {
        try {
            CompletableFuture<?> send = eventSender.send(topic, phoneNumber, message);
            if (send == null) {
                throw new IllegalStateException("Kafka sender returned no completion result");
            }
            send.whenComplete((result, error) -> {
                if (error != null) dispatchDirectly(phoneNumber, message, unwrap(error));
            });
        } catch (RuntimeException exception) {
            dispatchDirectly(phoneNumber, message, exception);
        }
    }

    private void dispatchDirectly(String phoneNumber, Object message, Throwable kafkaException) {
        log.warn("Kafka broker unavailable. Dispatching WhatsApp message directly via Meta API; "
                        + "recipient ending in {}",
                lastFourDigits(phoneNumber), kafkaException);
        try {
            CompletableFuture.runAsync(() -> directMessageSender.send(message))
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.error("Direct WhatsApp delivery failed for recipient ending in {}",
                                    lastFourDigits(phoneNumber), unwrap(error));
                        }
                    });
        } catch (RuntimeException exception) {
            log.error("Could not schedule direct WhatsApp delivery for recipient ending in {}",
                    lastFourDigits(phoneNumber), exception);
        }
    }

    private Throwable unwrap(Throwable error) {
        return error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
    }

    private String lastFourDigits(String phoneNumber) {
        String digits = phoneNumber == null ? "" : phoneNumber.replaceAll("\\D", "");
        return digits.substring(Math.max(0, digits.length() - 4));
    }
}
