package com.chaula.dental.controller;

import com.chaula.dental.service.WhatsAppWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks/whatsapp")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookController {
    private final WhatsAppWebhookService webhookService;

    @Value("${whatsapp.webhook.verify-token:}")
    private String verifyToken;

    @GetMapping
    public String verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        return webhookService.verifySubscription(mode, token, challenge, verifyToken);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> receive(
            @RequestBody byte[] payload,
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature
    ) {
        log.info("Received Meta WhatsApp webhook payload");
        try {
            webhookService.receive(payload, signature);
            return ResponseEntity.ok("EVENT_RECEIVED");
        } catch (ResponseStatusException exception) {
            log.warn("WhatsApp webhook rejected with HTTP {}: {}",
                    exception.getStatusCode().value(), exception.getReason());
            return ResponseEntity.status(exception.getStatusCode()).body("Webhook rejected");
        }
    }
}
