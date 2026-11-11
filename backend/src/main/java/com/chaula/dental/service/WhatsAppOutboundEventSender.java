package com.chaula.dental.service;

import java.util.concurrent.CompletableFuture;

public interface WhatsAppOutboundEventSender {
    CompletableFuture<?> send(String topic, String key, Object event);
}
