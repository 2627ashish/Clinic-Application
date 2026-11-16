package com.chaula.dental.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clinic")
public class ClinicConfigController {
    private final String videoCallNumber;

    public ClinicConfigController(
            @Value("${clinic.doctor.video-call-number:919810764715}") String videoCallNumber
    ) {
        this.videoCallNumber = videoCallNumber;
    }

    @GetMapping("/config")
    public ClinicConfig config() {
        return new ClinicConfig(videoCallNumber);
    }

    public record ClinicConfig(String videoCallNumber) {
    }
}
