package com.chaula.dental.service;

@FunctionalInterface
public interface WhatsAppDirectMessageSender {
    void send(Object message);
}
