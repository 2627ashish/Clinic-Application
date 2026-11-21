package com.chaula.dental.service;

import com.chaula.dental.messaging.event.WhatsAppListRow;
import com.chaula.dental.messaging.event.WhatsAppOutboundMessage;

import java.util.List;
import java.util.Map;

final class MetaWhatsAppPayloadFactory {
    private static final String HELLO_WORLD_TEMPLATE = "hello_world";

    private MetaWhatsAppPayloadFactory() {
    }

    static boolean isHelloWorldTemplate(WhatsAppOutboundMessage message) {
        return message.hasTemplate() && HELLO_WORLD_TEMPLATE.equals(message.templateName());
    }

    static Map<String, Object> textPayload(WhatsAppOutboundMessage message, String recipient) {
        return Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", recipient,
                "type", "text",
                "text", Map.of("preview_url", false, "body", message.text())
        );
    }

    static Map<String, Object> helloWorldTemplatePayload(String recipient) {
        return Map.of(
                "messaging_product", "whatsapp",
                "to", recipient,
                "type", "template",
                "template", Map.of(
                        "name", HELLO_WORLD_TEMPLATE,
                        "language", Map.of("code", "en_US")
                )
        );
    }

    static Map<String, Object> interactivePayload(WhatsAppOutboundMessage message, String recipient) {
        List<Map<String, String>> rows = message.listRows().stream()
                .limit(10)
                .map(MetaWhatsAppPayloadFactory::rowPayload)
                .toList();
        Map<String, Object> section = Map.of("title", "Available times", "rows", rows);
        Map<String, Object> interactive = Map.of(
                "type", "list",
                "header", Map.of("type", "text", "text", message.listTitle()),
                "body", Map.of("text", message.text()),
                "action", Map.of("button", message.listButton(), "sections", List.of(section))
        );
        return Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", recipient,
                "type", "interactive",
                "interactive", interactive
        );
    }

    private static Map<String, String> rowPayload(WhatsAppListRow row) {
        return Map.of(
                "id", row.id(),
                "title", row.title(),
                "description", row.description()
        );
    }
}
