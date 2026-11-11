package com.chaula.dental.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class KafkaWhatsAppOutboundEventSender implements WhatsAppOutboundEventSender {
    private final KafkaTemplate<Object, Object> kafkaTemplate;

    @Override
    public CompletableFuture<?> send(String topic, String key, Object event) {
        return kafkaTemplate.send(topic, key, event);
    }
}
