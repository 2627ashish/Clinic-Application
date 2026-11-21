package com.chaula.dental.service;

import com.chaula.dental.dto.AuthResponse;
import com.chaula.dental.dto.OtpResponse;

public interface PatientAuthService {
    OtpResponse requestOtp(String rawPhone);

    AuthResponse verifyOtp(String rawPhone, String enteredOtp);
}
