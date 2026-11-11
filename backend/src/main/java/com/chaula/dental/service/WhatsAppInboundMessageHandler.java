package com.chaula.dental.service;

import com.chaula.dental.messaging.event.WhatsAppInboundMessage;

@FunctionalInterface
public interface WhatsAppInboundMessageHandler {
    void process(WhatsAppInboundMessage message);
}
