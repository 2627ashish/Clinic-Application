package com.chaula.dental.messaging.event;

public record WhatsAppInboundMessage(
        String messageId,
        String phoneNumber,
        String text,
        String replyId
) {
}
