package com.chaula.dental.messaging.event;

public record WhatsAppDocumentMessage(
        String phoneNumber,
        String fileName,
        String caption,
        byte[] document
) {
    public WhatsAppDocumentMessage {
        document = document == null ? new byte[0] : document.clone();
    }

    @Override
    public byte[] document() {
        return document.clone();
    }
}
