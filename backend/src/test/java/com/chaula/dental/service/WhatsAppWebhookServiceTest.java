package com.chaula.dental.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
class WhatsAppWebhookServiceTest {
    @Test
    void ignoresStatusReceiptsAndEntriesWithoutChanges() {
        WhatsAppWebhookService service = requiredSignatureService();
        byte[] statusPayload = """
                {
                  "object": "whatsapp_business_account",
                  "entry": [
                    {"changes": [{"value": {"statuses": [{"status": "delivered"}]}}]},
                    {"id": "entry-without-changes"}
                  ]
                }
                """.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        service.receive(statusPayload, null);
    }

    @Test
    void ignoresEmptyOrMissingEntryArraysWithoutThrowing() {
        WhatsAppWebhookService service = requiredSignatureService();
        byte[] payload = """
                {"object": "whatsapp_business_account", "entry": [{"changes": null}, {}]}
                """.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        service.receive(payload, null);
    }

    @Test
    void acceptsUnsignedWebhookOnlyWhenLocalSignatureRequirementIsDisabled() {
        WhatsAppWebhookService service = new WhatsAppWebhookService(new ObjectMapper(), null);
        ReflectionTestUtils.setField(service, "signatureRequired", false);
        ReflectionTestUtils.setField(service, "appSecret", "local-test-secret");
        byte[] emptyValidWebhook = "{\"object\":\"whatsapp_business_account\",\"entry\":[]}"
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);

        service.receive(emptyValidWebhook, null);
    }

    @Test
    void rejectsUnsignedWebhookWhenSignatureIsRequired() {
        WhatsAppWebhookService service = new WhatsAppWebhookService(new ObjectMapper(), null);
        ReflectionTestUtils.setField(service, "signatureRequired", true);
        ReflectionTestUtils.setField(service, "appSecret", "test-secret");
        byte[] messageWebhook = """
                {"object":"whatsapp_business_account","entry":[{"changes":[{"value":{"messages":[{"from":"919810764715","id":"message-1","type":"text","text":{"body":"Hi"}}]}}]}]}
                """
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);

        assertThatThrownBy(() -> service.receive(messageWebhook, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid webhook signature");
    }

    @Test
    void acceptsMetaSignatureCalculatedFromExactRawPayload() throws Exception {
        WhatsAppWebhookService service = new WhatsAppWebhookService(new ObjectMapper(), null);
        ReflectionTestUtils.setField(service, "signatureRequired", true);
        ReflectionTestUtils.setField(service, "appSecret", " test-secret ");
        byte[] payload = """
                {"object":"whatsapp_business_account","entry":[{"changes":[{"value":{
                  "statuses":[{"status":"delivered"}],
                  "messages":[{"from":"919810764715","id":"message-1","type":"text","text":{"body":"Hi"}}]
                }}]}]}
                """.getBytes(StandardCharsets.UTF_8);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload));

        service.receive(payload, signature);
    }

    @Test
    void reportsMissingAppSecretAsServerConfigurationError() {
        WhatsAppWebhookService service = new WhatsAppWebhookService(new ObjectMapper(), null);
        ReflectionTestUtils.setField(service, "signatureRequired", true);
        ReflectionTestUtils.setField(service, "appSecret", "");
        byte[] messageWebhook = """
                {"object":"whatsapp_business_account","entry":[{"changes":[{"value":{"messages":[{"from":"919810764715","id":"message-1","type":"text","text":{"body":"Hi"}}]}}]}]}
                """
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);

        assertThatThrownBy(() -> service.receive(messageWebhook, "sha256=invalid"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("set WHATSAPP_APP_SECRET");
    }

    private WhatsAppWebhookService requiredSignatureService() {
        WhatsAppWebhookService service = new WhatsAppWebhookService(new ObjectMapper(), null);
        ReflectionTestUtils.setField(service, "signatureRequired", true);
        ReflectionTestUtils.setField(service, "appSecret", "");
        return service;
    }
}
