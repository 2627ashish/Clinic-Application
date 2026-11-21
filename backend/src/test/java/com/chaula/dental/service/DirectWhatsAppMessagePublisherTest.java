package com.chaula.dental.service;

import com.chaula.dental.messaging.event.WhatsAppDocumentMessage;
import com.chaula.dental.messaging.event.WhatsAppImageMessage;
import com.chaula.dental.messaging.event.WhatsAppListRow;
import com.chaula.dental.messaging.event.WhatsAppLocationMessage;
import com.chaula.dental.messaging.event.WhatsAppOutboundMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DirectWhatsAppMessagePublisherTest {
    @Test
    void dispatchesTextListTemplateDocumentImageAndLocationMessages() {
        List<Object> sentMessages = new ArrayList<>();
        DirectWhatsAppMessagePublisher publisher = new DirectWhatsAppMessagePublisher(sentMessages::add);
        byte[] content = {1, 2, 3};

        publisher.publishText("919810764715", "Booking received");
        publisher.publishList("919810764715", "Choose", "Clinic", "Select", List.of(
                new WhatsAppListRow("clinic|SEC_44", "Clinic 1", "Sec-44")));
        publisher.publish(WhatsAppOutboundMessage.template(
                "919810764715", "", "hello_world", "en_US", List.of()));
        publisher.publishDocument("919810764715", "visit.pdf", "Visit summary", content);
        publisher.publishImage("919810764715", "Prescription", "rx.png", "image/png", content);
        publisher.publishLocation("919810764715", "Clinic", "Noida", 28.5, 77.3);

        assertThat(sentMessages).hasSize(6);
        assertThat(sentMessages).contains(
                WhatsAppOutboundMessage.text("919810764715", "Booking received"),
                WhatsAppOutboundMessage.template("919810764715", "", "hello_world", "en_US", List.of()),
                new WhatsAppLocationMessage("919810764715", "Clinic", "Noida", 28.5, 77.3));
        assertThat(sentMessages).anySatisfy(message -> {
            assertThat(message).isInstanceOf(WhatsAppDocumentMessage.class);
            assertThat(((WhatsAppDocumentMessage) message).fileName()).isEqualTo("visit.pdf");
        });
        assertThat(sentMessages).anySatisfy(message -> {
            assertThat(message).isInstanceOf(WhatsAppImageMessage.class);
            assertThat(((WhatsAppImageMessage) message).fileName()).isEqualTo("rx.png");
        });
        assertThat(sentMessages).anySatisfy(message -> {
            assertThat(message).isInstanceOf(WhatsAppOutboundMessage.class);
            assertThat(((WhatsAppOutboundMessage) message).hasList()).isTrue();
        });
    }
}
