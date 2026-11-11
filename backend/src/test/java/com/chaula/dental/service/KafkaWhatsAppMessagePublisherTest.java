package com.chaula.dental.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaWhatsAppMessagePublisherTest {
    @Test
    void brokerFailureDoesNotEscapeOutboundPublishCall() {
        WhatsAppOutboundEventSender eventSender = mock(WhatsAppOutboundEventSender.class);
        when(eventSender.send(anyString(), anyString(), any()))
                .thenThrow(new IllegalStateException("broker unavailable"));
        KafkaWhatsAppMessagePublisher publisher = new KafkaWhatsAppMessagePublisher(eventSender);

        assertThatCode(() -> publisher.publishText("+919810764715", "Booking received"))
                .doesNotThrowAnyException();
    }

    @Test
    void asynchronousBrokerFailureDoesNotEscapeOutboundPublishCall() {
        WhatsAppOutboundEventSender eventSender = mock(WhatsAppOutboundEventSender.class);
        when(eventSender.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker unavailable")));
        KafkaWhatsAppMessagePublisher publisher = new KafkaWhatsAppMessagePublisher(eventSender);

        assertThatCode(() -> publisher.publishText("+919810764715", "Booking received"))
                .doesNotThrowAnyException();
    }
}
