package com.chaula.dental.service;

import com.chaula.dental.messaging.event.WhatsAppInboundMessage;
import com.chaula.dental.repository.PatientRepository;
import com.chaula.dental.util.PhoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.kafka.annotation.KafkaListener;

@Component
@RequiredArgsConstructor
public class WhatsAppInboundConsumer {
    private final WhatsAppSessionService sessionService;
    private final WhatsAppConversationService conversationService;
    private final PatientRepository patientRepository;
    private final WhatsAppNotificationService whatsappService;

    @KafkaListener(
            topics = "${whatsapp.kafka.inbound-topic:whatsapp-inbound}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(WhatsAppInboundMessage inbound) {
        String preferenceCommand = marketingPreferenceCommand(inbound);
        if (preferenceCommand != null) {
            updateMarketingPreference(inbound.phoneNumber(), preferenceCommand.equals("START"));
            return;
        }
        if (isRescheduleMessage(inbound)) {
            sessionService.clear(inbound.phoneNumber());
            conversationService.handle(inbound);
            return;
        }
        sessionService.handle(inbound);
    }

    private String marketingPreferenceCommand(WhatsAppInboundMessage inbound) {
        String text = inbound.text() == null ? "" : inbound.text().trim();
        if (text.equalsIgnoreCase("STOP") || text.equalsIgnoreCase("UNSUBSCRIBE")) return "STOP";
        if (text.equalsIgnoreCase("START")) return "START";
        return null;
    }

    private void updateMarketingPreference(String phoneNumber, boolean optedIn) {
        String normalized;
        try {
            normalized = PhoneUtils.normalizeIndianPhone(phoneNumber);
        } catch (IllegalArgumentException exception) {
            return;
        }
        var patient = patientRepository.findByPhoneNumber(normalized)
                .or(() -> patientRepository.findFirstByPhoneNumberEndingWith(
                        normalized.substring(normalized.length() - 10)));
        patient.ifPresent(account -> {
            account.setWhatsappMarketingOptIn(optedIn);
            account.setWhatsappMarketingOptInAt(optedIn ? java.time.Instant.now() : null);
            patientRepository.save(account);
            whatsappService.sendTextMessageAsync(account.getPhoneNumber(), optedIn
                    ? "You are subscribed to clinic WhatsApp updates. Reply STOP any time to opt out."
                    : "You are unsubscribed from clinic WhatsApp updates.");
        });
    }

    private boolean isRescheduleMessage(WhatsAppInboundMessage inbound) {
        String text = inbound.text() == null ? "" : inbound.text().trim();
        String replyId = inbound.replyId() == null ? "" : inbound.replyId().trim();
        return text.equalsIgnoreCase("RESCHEDULE")
                || replyId.startsWith("reschedule|")
                || replyId.startsWith("more|");
    }
}
