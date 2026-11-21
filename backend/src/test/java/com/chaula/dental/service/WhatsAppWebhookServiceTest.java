package com.chaula.dental.service;

import com.chaula.dental.messaging.event.WhatsAppInboundMessage;
import com.chaula.dental.messaging.event.WhatsAppOutboundMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
                """.getBytes(StandardCharsets.UTF_8);

        service.receive(statusPayload, null);
    }

    @Test
    void ignoresEmptyOrMissingEntryArraysWithoutThrowing() {
        WhatsAppWebhookService service = requiredSignatureService();
        byte[] payload = """
                {"object": "whatsapp_business_account", "entry": [{"changes": null}, {}]}
                """.getBytes(StandardCharsets.UTF_8);

        service.receive(payload, null);
    }

    @Test
    void acceptsUnsignedWebhookOnlyWhenLocalSignatureRequirementIsDisabled() {
        WhatsAppWebhookService service = service(message -> { }, mock(WhatsAppMessagePublisher.class));
        ReflectionTestUtils.setField(service, "signatureRequired", false);
        ReflectionTestUtils.setField(service, "appSecret", "local-test-secret");
        byte[] emptyValidWebhook = "{\"object\":\"whatsapp_business_account\",\"entry\":[]}"
                .getBytes(StandardCharsets.UTF_8);

        service.receive(emptyValidWebhook, null);
    }

    @Test
    void rejectsUnsignedWebhookWhenSignatureIsRequired() {
        WhatsAppWebhookService service = requiredSignatureService();
        byte[] messageWebhook = messagePayload();

        assertThatThrownBy(() -> service.receive(messageWebhook, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid webhook signature");
    }

    @Test
    void acceptsMetaSignatureCalculatedFromExactRawPayload() throws Exception {
        WhatsAppWebhookService service = requiredSignatureService();
        byte[] payload = """
                {"object":"whatsapp_business_account","entry":[{"changes":[{"value":{
                  "statuses":[{"status":"delivered"}],
                  "messages":[{"from":"919810764715","id":"message-1","type":"text","text":{"body":"Hi"}}]
                }}]}]}
                """.getBytes(StandardCharsets.UTF_8);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload));
        ReflectionTestUtils.setField(service, "appSecret", " test-secret ");

        service.receive(payload, signature);
    }

    @Test
    void reportsMissingAppSecretAsServerConfigurationError() {
        WhatsAppWebhookService service = requiredSignatureService();
        ReflectionTestUtils.setField(service, "appSecret", "");

        assertThatThrownBy(() -> service.receive(messagePayload(), "sha256=invalid"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("set WHATSAPP_APP_SECRET");
    }

    @Test
    void dispatchesInboundMessageToTheAsyncHandler() {
        List<WhatsAppInboundMessage> processedMessages = new ArrayList<>();
        WhatsAppWebhookService service = service(processedMessages::add, mock(WhatsAppMessagePublisher.class));
        ReflectionTestUtils.setField(service, "signatureRequired", false);

        service.receive(messagePayload(), null);

        assertThat(processedMessages).containsExactly(new WhatsAppInboundMessage(
                "message-1", "919810764715", "Hi", ""));
    }

    @Test
    void queuesHelloWorldTemplateWhenMetaReportsReengagementFailure() {
        WhatsAppMessagePublisher publisher = mock(WhatsAppMessagePublisher.class);
        WhatsAppWebhookService service = service(null, publisher);
        ReflectionTestUtils.setField(service, "signatureRequired", false);
        byte[] payload = """
                {"object":"whatsapp_business_account","entry":[{"changes":[{"value":{"statuses":[
                  {"id":"outbound-message-1","status":"failed","recipient_id":"919810764715","errors":[{"code":131047,"title":"Re-engagement message"}]}
                ]}}]}]}
                """.getBytes(StandardCharsets.UTF_8);

        service.receive(payload, null);
        service.receive(payload, null);

        verify(publisher).publish(WhatsAppOutboundMessage.template(
                "919810764715", "", "hello_world", "en_US", List.of()));
    }

    private WhatsAppWebhookService service(
            WhatsAppInboundMessageHandler inboundHandler,
            WhatsAppMessagePublisher messagePublisher
    ) {
        return new WhatsAppWebhookService(new ObjectMapper(), inboundHandler, messagePublisher);
    }

    private WhatsAppWebhookService requiredSignatureService() {
        WhatsAppWebhookService service = service(message -> { }, mock(WhatsAppMessagePublisher.class));
        ReflectionTestUtils.setField(service, "signatureRequired", true);
        ReflectionTestUtils.setField(service, "appSecret", "test-secret");
        return service;
    }

    private byte[] messagePayload() {
        return """
                {"object":"whatsapp_business_account","entry":[{"changes":[{"value":{"messages":[{"from":"919810764715","id":"message-1","type":"text","text":{"body":"Hi"}}]}}]}]}
                """
                .getBytes(StandardCharsets.UTF_8);
    }
}
