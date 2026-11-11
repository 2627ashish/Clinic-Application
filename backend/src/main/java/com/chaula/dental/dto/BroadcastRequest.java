package com.chaula.dental.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BroadcastRequest(
        @NotBlank @Size(max = 100) String campaignTitle,
        @NotBlank @Size(max = 1024) String message
) {
}
