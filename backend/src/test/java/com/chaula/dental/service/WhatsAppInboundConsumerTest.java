package com.chaula.dental.service;

import com.chaula.dental.domain.Patient;
import com.chaula.dental.messaging.event.WhatsAppInboundMessage;
import com.chaula.dental.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhatsAppInboundConsumerTest {
    @Mock
    private PatientRepository patientRepository;

    @Mock
    private WhatsAppNotificationService whatsappService;

    private WhatsAppInboundConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new WhatsAppInboundConsumer(null, null, patientRepository, whatsappService);
    }

    @Test
    void stopCommandDisablesMarketingWithoutStartingConversationFlow() {
        Patient patient = Patient.builder().phoneNumber("919876543210").whatsappMarketingOptIn(true).build();
        when(patientRepository.findByPhoneNumber("919876543210")).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        consumer.consume(new WhatsAppInboundMessage("message-id", "919876543210", "STOP", ""));

        assertThat(patient.isWhatsappMarketingOptIn()).isFalse();
        verify(patientRepository).save(patient);
        verify(whatsappService).sendTextMessageAsync(
                patient.getPhoneNumber(), "You are unsubscribed from clinic WhatsApp updates.");
    }

    @Test
    void startCommandExplicitlyOptsPatientBackIn() {
        Patient patient = Patient.builder().phoneNumber("919876543210").whatsappMarketingOptIn(false).build();
        when(patientRepository.findByPhoneNumber("919876543210")).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        consumer.consume(new WhatsAppInboundMessage("message-id", "919876543210", "START", ""));

        assertThat(patient.isWhatsappMarketingOptIn()).isTrue();
        assertThat(patient.getWhatsappMarketingOptInAt()).isNotNull();
        verify(whatsappService).sendTextMessageAsync(
                patient.getPhoneNumber(), "You are subscribed to clinic WhatsApp updates. Reply STOP any time to opt out.");
    }
}
