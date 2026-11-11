package com.chaula.dental.service;

import com.chaula.dental.util.PhoneUtils;
import com.chaula.dental.messaging.event.WhatsAppListRow;
import com.chaula.dental.messaging.event.WhatsAppOutboundMessage;
import com.chaula.dental.messaging.event.WhatsAppImageMessage;
import com.chaula.dental.messaging.event.WhatsAppLocationMessage;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MetaWhatsAppCloudApiClient {
    private final RestClient restClient;
    private final String accessToken;
    private final String phoneNumberId;

    public MetaWhatsAppCloudApiClient(
            RestClient.Builder builder,
            @Value("${whatsapp.api.url:https://graph.facebook.com/v21.0}") String apiUrl,
            @Value("${whatsapp.api.token:${whatsapp.token:}}") String accessToken,
            @Value("${whatsapp.api.phone-number-id:${whatsapp.phone-number-id:}}") String phoneNumberId
    ) {
        // Normalize baseUrl so it always points to https://graph.facebook.com/v21.0
        String cleanBaseUrl = apiUrl.replaceAll("/+$", "");
        if (!cleanBaseUrl.contains("/v")) {
            cleanBaseUrl += "/v21.0";
        }

        this.restClient = builder.baseUrl(cleanBaseUrl).build();
        this.accessToken = accessToken != null ? accessToken.trim() : "";
        this.phoneNumberId = phoneNumberId != null ? phoneNumberId.trim() : "";

        log.info("Initialized Meta WhatsApp Client with BaseUrl: {} and PhoneID: {}", cleanBaseUrl, this.phoneNumberId);
    }

    public void send(WhatsAppOutboundMessage message) {
        requireConfiguration();

        Map<String, Object> payload = message.hasList()
                ? interactivePayload(message)
                : textPayload(message);

        String uri = "/" + phoneNumberId + "/messages";

        try {
            log.info("Sending WhatsApp message via URI '{}'", uri);

            restClient.post()
                    .uri(uri)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("WhatsApp message successfully accepted by Meta");

        } catch (RestClientResponseException exception) {
            log.error("Meta rejected WhatsApp payload [HTTP {}]: {}",
                    exception.getStatusCode().value(),
                    exception.getResponseBodyAsString());
            throw deliveryException(exception);
        }
    }

    public void sendPdfDocument(String recipientPhone, byte[] pdfBytes, String fileName, String caption) {
        requireConfiguration();
        if (pdfBytes == null || pdfBytes.length == 0 || fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("A PDF file and filename are required");
        }
        String recipient = normalizePhone(recipientPhone);
        try {
            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            form.add("messaging_product", "whatsapp");
            form.add("type", "application/pdf");
            ByteArrayResource pdfResource = new ByteArrayResource(pdfBytes) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };
            HttpHeaders fileHeaders = new HttpHeaders();
            fileHeaders.setContentType(MediaType.APPLICATION_PDF);
            form.add("file", new HttpEntity<>(pdfResource, fileHeaders));
            JsonNode media = restClient.post()
                    .uri("/" + phoneNumberId + "/media")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);
            String mediaId = media == null ? "" : media.path("id").asText("");
            if (mediaId.isBlank()) {
                throw new IllegalStateException("Meta media upload response did not contain a media id");
            }

            Map<String, Object> payload = Map.of(
                    "messaging_product", "whatsapp",
                    "recipient_type", "individual",
                    "to", recipient,
                    "type", "document",
                    "document", Map.of(
                            "id", mediaId,
                            "filename", fileName,
                            "caption", caption == null ? "" : caption
                    )
            );
            restClient.post()
                    .uri("/" + phoneNumberId + "/messages")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("WhatsApp PDF document accepted by Meta for recipient ending in {}",
                    recipient.substring(recipient.length() - 4));
        } catch (RestClientResponseException exception) {
            log.error("Meta rejected WhatsApp document [HTTP {}]: {}",
                    exception.getStatusCode().value(), exception.getResponseBodyAsString());
            throw deliveryException(exception);
        }
    }

    public void sendImage(WhatsAppImageMessage message) {
        requireConfiguration();
        byte[] imageBytes = message.image();
        MediaType contentType = MediaType.parseMediaType(message.contentType());
        if (imageBytes.length == 0 || imageBytes.length > 1024 * 1024
                || !(MediaType.IMAGE_JPEG.equals(contentType) || MediaType.IMAGE_PNG.equals(contentType))) {
            throw new IllegalArgumentException("Only JPEG or PNG images up to 1 MB can be sent.");
        }
        String recipient = normalizePhone(message.phoneNumber());
        try {
            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            form.add("messaging_product", "whatsapp");
            form.add("type", contentType.toString());
            ByteArrayResource imageResource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return safeFileName(message.fileName());
                }
            };
            HttpHeaders fileHeaders = new HttpHeaders();
            fileHeaders.setContentType(contentType);
            form.add("file", new HttpEntity<>(imageResource, fileHeaders));
            JsonNode media = restClient.post()
                    .uri("/" + phoneNumberId + "/media")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);
            String mediaId = media == null ? "" : media.path("id").asText("");
            if (mediaId.isBlank()) {
                throw new IllegalStateException("Meta media upload response did not contain a media id");
            }
            Map<String, Object> payload = Map.of(
                    "messaging_product", "whatsapp",
                    "recipient_type", "individual",
                    "to", recipient,
                    "type", "image",
                    "image", Map.of("id", mediaId, "caption", message.caption())
            );
            restClient.post()
                    .uri("/" + phoneNumberId + "/messages")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("WhatsApp image accepted by Meta for recipient ending in {}",
                    recipient.substring(recipient.length() - 4));
        } catch (RestClientResponseException exception) {
            log.error("Meta rejected WhatsApp image [HTTP {}]: {}",
                    exception.getStatusCode().value(), exception.getResponseBodyAsString());
            throw deliveryException(exception);
        }
    }

    public void sendLocation(WhatsAppLocationMessage message) {
        requireConfiguration();
        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", normalizePhone(message.phoneNumber()),
                "type", "location",
                "location", Map.of(
                        "latitude", message.latitude(),
                        "longitude", message.longitude(),
                        "name", message.name(),
                        "address", message.address()
                )
        );
        try {
            restClient.post()
                    .uri("/" + phoneNumberId + "/messages")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            log.error("Meta rejected WhatsApp location [HTTP {}]: {}",
                    exception.getStatusCode().value(), exception.getResponseBodyAsString());
            throw deliveryException(exception);
        }
    }

    private String safeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) return "campaign-image";
        return fileName.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private Map<String, Object> textPayload(WhatsAppOutboundMessage message) {
        return Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", normalizePhone(message.phoneNumber()),
                "type", "text",
                "text", Map.of("preview_url", false, "body", message.text())
        );
    }

    private Map<String, Object> interactivePayload(WhatsAppOutboundMessage message) {
        List<Map<String, String>> rows = message.listRows().stream()
                .limit(10)
                .map(this::rowPayload)
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
                "to", normalizePhone(message.phoneNumber()),
                "type", "interactive",
                "interactive", interactive
        );
    }

    private Map<String, String> rowPayload(WhatsAppListRow row) {
        return Map.of(
                "id", row.id(),
                "title", row.title(),
                "description", row.description()
        );
    }

    private String normalizePhone(String phoneNumber) {
        try {
            return PhoneUtils.normalizeIndianPhone(phoneNumber);
        } catch (IllegalArgumentException exception) {
            throw new WhatsAppPermanentDeliveryException("Recipient phone number is invalid.", exception);
        }
    }

    private RuntimeException deliveryException(RestClientResponseException exception) {
        if (exception.getStatusCode().is4xxClientError()
                && exception.getStatusCode().value() != HttpStatus.TOO_MANY_REQUESTS.value()) {
            return new WhatsAppPermanentDeliveryException(
                    "WhatsApp Cloud API rejected the message with HTTP "
                            + exception.getStatusCode().value(), exception);
        }
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "WhatsApp Cloud API rejected the message.");
    }

    private void requireConfiguration() {
        if (accessToken.isBlank() || phoneNumberId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "WhatsApp Cloud API is not configured; set WHATSAPP_API_TOKEN and WHATSAPP_PHONE_NUMBER_ID.");
        }
    }
}