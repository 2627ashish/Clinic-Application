package com.chaula.dental.service;

import com.chaula.dental.domain.Patient;
import com.chaula.dental.dto.BroadcastRequest;
import com.chaula.dental.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {
    @Mock
    private PatientRepository patientRepository;

    @Mock
    private WhatsAppNotificationService whatsappService;

    @InjectMocks
    private CampaignService campaignService;

    @Test
    void broadcastsTextOnlyToExplicitlyOptedInPatients() {
        Patient optedIn = Patient.builder().phoneNumber("919876543210").whatsappMarketingOptIn(true).build();
        when(patientRepository.findByWhatsappMarketingOptInTrue()).thenReturn(List.of(optedIn));

        campaignService.broadcast(new BroadcastRequest("Clinic update", "We will be closed tomorrow."), null);

        verify(whatsappService).sendTextMessageAsync(optedIn.getPhoneNumber(), "We will be closed tomorrow.");
        verify(patientRepository).findByWhatsappMarketingOptInTrue();
    }

    @Test
    void rejectsSpoofedImageContentBeforeSendingAnything() {
        MockMultipartFile fakePng = new MockMultipartFile(
                "image", "picture.png", "image/png", "not a png".getBytes());

        assertThatThrownBy(() -> campaignService.broadcast(
                new BroadcastRequest("Clinic update", "Hello"), fakePng))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(whatsappService);
    }
}
