package com.chaula.dental.dto;

public record OtpResponse(boolean success, String message, int resendCooldown, String mockOtp) {
}
