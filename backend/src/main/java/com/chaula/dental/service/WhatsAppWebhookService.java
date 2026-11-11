package com.chaula.dental.service;

import com.chaula.dental.messaging.event.WhatsAppInboundMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookService {
    private final ObjectMapper objectMapper;
    private final WhatsAppInboundEventSender eventSender;
    private final WhatsAppInboundMessageHandler inboundMessageHandler;

    @Value("${whatsapp.webhook.app-secret:}")
    private String appSecret;

    @Value("${whatsapp.webhook.signature-required:true}")
    private boolean signatureRequired;

    @Value("${whatsapp.kafka.inbound-topic:whatsapp-inbound}")
    private String inboundTopic;

    public String verifySubscription(String mode, String token, String challenge, String expectedToken) {
        if (!"subscribe".equals(mode) || challenge == null || !matches(expectedToken, token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Webhook verification failed.");
        }
        return challenge;
    }

    public void receive(byte[] payload, String signature) {
        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid WhatsApp webhook payload.");
        }
        if (root == null || !root.isObject()
                || !"whatsapp_business_account".equals(root.path("object").asText())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid WhatsApp webhook object.");
        }
        if (!containsInboundMessages(root)) return;
        if (signatureRequired || signature != null) {
            verifySignature(payload, signature);
        }
        try {
            JsonNode entries = root.path("entry");
            if (!entries.isArray()) return;
            for (JsonNode entry : entries) {
                JsonNode changes = entry.path("changes");
                if (!changes.isArray()) continue;
                for (JsonNode change : changes) {
                    JsonNode value = change.path("value");
                    JsonNode statuses = value.path("statuses");
                    if (statuses.isArray() && !statuses.isEmpty()) {
                        continue;
                    }
                    JsonNode messages = value.path("messages");
                    if (!messages.isArray()) continue;
                    for (JsonNode message : messages) {
                        publishIncoming(message);
                    }
                }
            }
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not queue or process the WhatsApp message. Meta can retry the webhook.", exception);
        }
    }

    private boolean containsInboundMessages(JsonNode root) {
        JsonNode entries = root.path("entry");
        if (!entries.isArray()) return false;
        for (JsonNode entry : entries) {
            JsonNode changes = entry.path("changes");
            if (!changes.isArray()) continue;
            for (JsonNode change : changes) {
                JsonNode messages = change.path("value").path("messages");
                if (messages.isArray() && !messages.isEmpty()) return true;
            }
        }
        return false;
    }

    private void publishIncoming(JsonNode message) throws Exception {
        String phoneNumber = message.path("from").asText("");
        String messageId = message.path("id").asText("");
        String messageType = message.path("type").asText("");
        String text = "text".equals(messageType)
                ? message.path("text").path("body").asText("")
                : messageType.isBlank() ? "" : "[Non-text message: " + messageType + "]";
        String replyId = message.path("interactive").path("list_reply").path("id").asText("");
        if (replyId.isBlank()) {
            replyId = message.path("interactive").path("button_reply").path("id").asText("");
        }
        if (phoneNumber.isBlank() || messageId.isBlank() || (text.isBlank() && replyId.isBlank())) return;

        WhatsAppInboundMessage event = new WhatsAppInboundMessage(messageId, phoneNumber, text, replyId);
        try {
            eventSender.send(inboundTopic, phoneNumber, event).get(6, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            processInboundDirectly(event, exception);
        } catch (ExecutionException | TimeoutException | RuntimeException exception) {
            processInboundDirectly(event, exception);
        }
    }

    private void processInboundDirectly(WhatsAppInboundMessage event, Exception kafkaException) {
        String digits = event.phoneNumber().replaceAll("\\D", "");
        String recipientSuffix = digits.substring(Math.max(0, digits.length() - 4));
        log.warn("Kafka broker unavailable. Processing inbound WhatsApp message directly; "
                        + "recipient ending in {}",
                recipientSuffix, kafkaException);
        inboundMessageHandler.process(event);
    }

    private void verifySignature(byte[] payload, String suppliedSignature) {
        String configuredSecret = appSecret == null ? "" : appSecret.trim();
        if (configuredSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "WhatsApp webhook signature validation is not configured; set WHATSAPP_APP_SECRET.");
        }
        if (suppliedSignature == null || !suppliedSignature.matches("sha256=[0-9a-fA-F]{64}")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature.");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(configuredSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(payload);
            byte[] supplied = HexFormat.of().parseHex(suppliedSignature.substring("sha256=".length()));
            if (!MessageDigest.isEqual(expected, supplied)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature.");
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to validate webhook signature.");
        }
    }

    private boolean matches(String expected, String supplied) {
        if (expected == null || expected.isBlank() || supplied == null) return false;
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8)
        );
    }
}
