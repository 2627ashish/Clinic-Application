package com.chaula.dental.service;

import com.chaula.dental.messaging.event.WhatsAppOutboundMessage;
import com.chaula.dental.messaging.event.WhatsAppImageMessage;
import com.chaula.dental.messaging.event.WhatsAppLocationMessage;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MetaWhatsAppCloudApiClientTest {
    @Test
    void sendsDigitsOnlyRecipientToVersionedMetaEndpoint() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MetaWhatsAppCloudApiClient client = new MetaWhatsAppCloudApiClient(
                builder, "https://graph.facebook.com", "test-token", "phone-id");
        server.expect(requestTo("https://graph.facebook.com/v21.0/phone-id/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "messaging_product": "whatsapp",
                          "recipient_type": "individual",
                          "to": "919810764715",
                          "type": "text",
                          "text": {"preview_url": false, "body": "Hello"}
                        }
                        """))
                .andRespond(withSuccess());

        client.send(WhatsAppOutboundMessage.text("+91-98107 64715", "Hello"));
        server.verify();
    }

    @Test
    void classifiesMetaBadRequestAsPermanentToAvoidRepeatedRetries() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MetaWhatsAppCloudApiClient client = new MetaWhatsAppCloudApiClient(
                builder, "https://graph.facebook.com", "test-token", "phone-id");
        server.expect(requestTo("https://graph.facebook.com/v21.0/phone-id/messages"))
                .andRespond(withBadRequest());

        assertThatThrownBy(() -> client.send(WhatsAppOutboundMessage.text("919810764715", "Hello")))
                .isInstanceOf(WhatsAppPermanentDeliveryException.class);
        server.verify();
    }

    @Test
    void sendsTextInsteadOfUnapprovedTemplateEvents() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MetaWhatsAppCloudApiClient client = new MetaWhatsAppCloudApiClient(
                builder, "https://graph.facebook.com", "test-token", "phone-id");
        server.expect(requestTo("https://graph.facebook.com/v21.0/phone-id/messages"))
                .andExpect(content().json("""
                        {
                          "messaging_product": "whatsapp",
                          "recipient_type": "individual",
                          "to": "919810764715",
                          "type": "text",
                          "text": {"preview_url": false, "body": "Appointment received"}
                        }
                        """))
                .andRespond(withSuccess());

        client.send(WhatsAppOutboundMessage.template(
                "919810764715", "Appointment received", "appointment_pending", "en_US", List.of("Asha")));
        server.verify();
    }

    @Test
    void uploadsPdfThenSendsDocumentUsingTheReturnedMediaId() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MetaWhatsAppCloudApiClient client = new MetaWhatsAppCloudApiClient(
                builder, "https://graph.facebook.com", "test-token", "phone-id");
        byte[] pdf = "%PDF-test".getBytes(StandardCharsets.UTF_8);
        server.expect(requestTo("https://graph.facebook.com/v21.0/phone-id/media"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", org.hamcrest.Matchers.startsWith("multipart/form-data;")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("messaging_product")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("application/pdf")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("visit-summary.pdf")))
                .andRespond(withSuccess("{\"id\":\"media-id\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://graph.facebook.com/v21.0/phone-id/messages"))
                .andExpect(content().json("""
                        {
                          "messaging_product": "whatsapp",
                          "recipient_type": "individual",
                          "to": "919810764715",
                          "type": "document",
                          "document": {
                            "id": "media-id",
                            "filename": "visit-summary.pdf",
                            "caption": "Visit summary"
                          }
                        }
                        """))
                .andRespond(withSuccess());

        client.sendPdfDocument("+91-98107 64715", pdf, "visit-summary.pdf", "Visit summary");
        server.verify();
    }

    @Test
    void uploadsPngThenSendsImageUsingTheReturnedMediaId() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MetaWhatsAppCloudApiClient client = new MetaWhatsAppCloudApiClient(
                builder, "https://graph.facebook.com", "test-token", "phone-id");
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        server.expect(requestTo("https://graph.facebook.com/v21.0/phone-id/media"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", org.hamcrest.Matchers.startsWith("multipart/form-data;")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("image/png")))
                .andRespond(withSuccess("{\"id\":\"image-media-id\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://graph.facebook.com/v21.0/phone-id/messages"))
                .andExpect(content().json("""
                        {
                          "messaging_product": "whatsapp",
                          "recipient_type": "individual",
                          "to": "919810764715",
                          "type": "image",
                          "image": {"id": "image-media-id", "caption": "Clinic update"}
                        }
                        """))
                .andRespond(withSuccess());

        client.sendImage(new WhatsAppImageMessage(
                "+91-98107 64715", "Clinic update", "image/png", "notice.png", png));
        server.verify();
    }

    @Test
    void sendsNativeLocationPinInsteadOfLongMapUrl() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MetaWhatsAppCloudApiClient client = new MetaWhatsAppCloudApiClient(
                builder, "https://graph.facebook.com", "test-token", "phone-id");
        server.expect(requestTo("https://graph.facebook.com/v21.0/phone-id/messages"))
                .andExpect(content().json("""
                        {
                          "messaging_product": "whatsapp",
                          "recipient_type": "individual",
                          "to": "919810764715",
                          "type": "location",
                          "location": {
                            "latitude": 28.5578734,
                            "longitude": 77.3439285,
                            "name": "Clinic 1",
                            "address": "Gali No. 3, Chhalera, Noida"
                          }
                        }
                        """))
                .andRespond(withSuccess());

        client.sendLocation(new WhatsAppLocationMessage(
                "+91-98107 64715", "Clinic 1", "Gali No. 3, Chhalera, Noida", 28.5578734, 77.3439285));
        server.verify();
    }
}
