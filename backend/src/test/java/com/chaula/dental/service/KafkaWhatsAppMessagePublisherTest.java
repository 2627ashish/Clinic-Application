package com.chaula.dental.service;

import com.chaula.dental.messaging.event.WhatsAppDocumentMessage;
import com.chaula.dental.messaging.event.WhatsAppImageMessage;
import com.chaula.dental.messaging.event.WhatsAppLocationMessage;
import com.chaula.dental.messaging.event.WhatsAppOutboundMessage;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaWhatsAppMessagePublisherTest {
    @Test
    void dispatchesTextDirectlyWhenKafkaSendFailsSynchronously() throws InterruptedException {
        List<Object> sentMessages = new CopyOnWriteArrayList<>();
        CountDownLatch directDispatch = new CountDownLatch(1);
        WhatsAppOutboundEventSender eventSender = (topic, key, event) -> {
            throw new IllegalStateException("broker unavailable");
        };
        KafkaWhatsAppMessagePublisher publisher = publisher(eventSender, sentMessages, directDispatch);

        WhatsAppOutboundMessage message = WhatsAppOutboundMessage.text("+919810764715", "Booking received");
        publisher.publishText(message.phoneNumber(), message.text());

        assertThat(directDispatch.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(sentMessages).containsExactly(message);
    }

    @Test
    void dispatchesTextDirectlyWhenKafkaSendFailsAsynchronously() throws InterruptedException {
        List<Object> sentMessages = new CopyOnWriteArrayList<>();
        CountDownLatch directDispatch = new CountDownLatch(1);
        WhatsAppOutboundEventSender eventSender = (topic, key, event) ->
                CompletableFuture.failedFuture(new IllegalStateException("broker unavailable"));
        KafkaWhatsAppMessagePublisher publisher = publisher(eventSender, sentMessages, directDispatch);

        WhatsAppOutboundMessage message = WhatsAppOutboundMessage.text("+919810764715", "Booking received");
        publisher.publishText(message.phoneNumber(), message.text());

        assertThat(directDispatch.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(sentMessages).containsExactly(message);
    }

    @Test
    void dispatchesDocumentImageAndLocationDirectlyWhenKafkaFails() throws InterruptedException {
        List<Object> sentMessages = new CopyOnWriteArrayList<>();
        CountDownLatch directDispatches = new CountDownLatch(3);
        WhatsAppOutboundEventSender eventSender = (topic, key, event) ->
                CompletableFuture.failedFuture(new IllegalStateException("broker unavailable"));
        KafkaWhatsAppMessagePublisher publisher = publisher(eventSender, sentMessages, directDispatches);
        byte[] content = {1, 2, 3};

        publisher.publishDocument("+919810764715", "visit.pdf", "Visit summary", content);
        publisher.publishImage("+919810764715", "Campaign", "campaign.png", "image/png", content);
        publisher.publishLocation("+919810764715", "Clinic", "Noida", 28.5, 77.3);

        assertThat(directDispatches.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(sentMessages).hasSize(3);
        assertThat(sentMessages).anySatisfy(message -> {
            assertThat(message).isInstanceOf(WhatsAppDocumentMessage.class);
            assertThat(((WhatsAppDocumentMessage) message).fileName()).isEqualTo("visit.pdf");
        });
        assertThat(sentMessages).anySatisfy(message -> {
            assertThat(message).isInstanceOf(WhatsAppImageMessage.class);
            assertThat(((WhatsAppImageMessage) message).fileName()).isEqualTo("campaign.png");
        });
        assertThat(sentMessages).contains(new WhatsAppLocationMessage(
                "+919810764715", "Clinic", "Noida", 28.5, 77.3));
    }

    private KafkaWhatsAppMessagePublisher publisher(
            WhatsAppOutboundEventSender eventSender,
            List<Object> sentMessages,
            CountDownLatch dispatches
    ) {
        WhatsAppDirectMessageSender directSender = message -> {
            sentMessages.add(message);
            dispatches.countDown();
        };
        KafkaWhatsAppMessagePublisher publisher =
                new KafkaWhatsAppMessagePublisher(eventSender, directSender);
        ReflectionTestUtils.setField(publisher, "outboundTopic", "whatsapp-outbound");
        ReflectionTestUtils.setField(publisher, "documentTopic", "whatsapp-outbound-documents");
        ReflectionTestUtils.setField(publisher, "imageTopic", "whatsapp-outbound-images");
        ReflectionTestUtils.setField(publisher, "locationTopic", "whatsapp-outbound-locations");
        return publisher;
    }
}
