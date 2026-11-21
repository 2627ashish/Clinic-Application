package com.chaula.dental.controller;

import com.chaula.dental.dto.AuthResponse;
import com.chaula.dental.dto.OtpResponse;
import com.chaula.dental.dto.PatientOtpRequest;
import com.chaula.dental.dto.PatientOtpVerifyRequest;
import com.chaula.dental.service.PatientAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/patient")
@RequiredArgsConstructor
public class PatientAuthController {
    private final PatientAuthService patientAuthService;

    @PostMapping("/request-otp")
    public OtpResponse requestOtp(@Valid @RequestBody PatientOtpRequest request) {
        return patientAuthService.requestOtp(request.phoneNumber());
    }

    @PostMapping("/verify-otp")
    public AuthResponse verifyOtp(@Valid @RequestBody PatientOtpVerifyRequest request) {
        return patientAuthService.verifyOtp(request.phoneNumber(), request.code());
    }
}
