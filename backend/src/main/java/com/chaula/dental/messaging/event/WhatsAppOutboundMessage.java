package com.chaula.dental.messaging.event;

import java.util.List;

public record WhatsAppOutboundMessage(
        String phoneNumber,
        String text,
        String listTitle,
        String listButton,
        List<WhatsAppListRow> listRows,
        String templateName,
        String templateLanguage,
        List<String> templateParameters
) {
    public static WhatsAppOutboundMessage text(String phoneNumber, String text) {
        return new WhatsAppOutboundMessage(phoneNumber, text, null, null, List.of(), null, null, List.of());
    }

    public static WhatsAppOutboundMessage list(
            String phoneNumber,
            String text,
            String title,
            String button,
            List<WhatsAppListRow> rows
    ) {
        return new WhatsAppOutboundMessage(phoneNumber, text, title, button, List.copyOf(rows), null, null, List.of());
    }

    public static WhatsAppOutboundMessage template(
            String phoneNumber,
            String fallbackText,
            String templateName,
            String language,
            List<String> parameters
    ) {
        return new WhatsAppOutboundMessage(
                phoneNumber, fallbackText, null, null, List.of(),
                templateName, language, List.copyOf(parameters)
        );
    }

    public boolean hasList() {
        return listRows != null && !listRows.isEmpty();
    }

    public boolean hasTemplate() {
        return templateName != null && !templateName.isBlank();
    }
}
