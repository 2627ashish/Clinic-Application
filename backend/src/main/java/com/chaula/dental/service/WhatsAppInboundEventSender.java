package com.chaula.dental.service;

import java.util.concurrent.CompletableFuture;

public interface WhatsAppInboundEventSender {
    CompletableFuture<?> send(String topic, String key, Object event);
}
