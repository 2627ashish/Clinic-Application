package com.chaula.dental.messaging.event;

public record WhatsAppLocationMessage(
        String phoneNumber,
        String name,
        String address,
        double latitude,
        double longitude
) {
}
