package com.chaula.dental.controller;

import com.chaula.dental.dto.DoctorLoginRequest;
import com.chaula.dental.dto.DoctorSetupRequest;
import com.chaula.dental.dto.DoctorSetupStatus;
import com.chaula.dental.dto.DoctorSessionResponse;
import com.chaula.dental.dto.PasswordChangeRequest;
import com.chaula.dental.service.DoctorAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/doctor/auth")
@RequiredArgsConstructor
public class DoctorAuthController {
    private final DoctorAuthService authService;

    @GetMapping("/setup-status")
    public DoctorSetupStatus setupStatus() {
        return new DoctorSetupStatus(authService.setupRequired());
    }

    @PostMapping("/setup")
    public DoctorSessionResponse setup(@Valid @RequestBody DoctorSetupRequest request) {
        return authService.setup(request.username(), request.password(), request.fullName(), request.email());
    }

    @PostMapping("/login")
    public DoctorSessionResponse login(@Valid @RequestBody DoctorLoginRequest request) {
        return authService.login(request.username(), request.password());
    }

    @PostMapping("/password")
    public void changePassword(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody PasswordChangeRequest request
    ) {
        authService.changePassword(bearerToken(authorization), request.currentPassword(), request.newPassword());
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return "";
        return authorization.substring(7);
    }
}
