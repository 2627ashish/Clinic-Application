package com.chaula.dental.messaging.event;

public record WhatsAppImageMessage(
        String phoneNumber,
        String caption,
        String contentType,
        String fileName,
        byte[] image
) {
    public WhatsAppImageMessage {
        image = image == null ? new byte[0] : image.clone();
    }

    @Override
    public byte[] image() {
        return image.clone();
    }
}
