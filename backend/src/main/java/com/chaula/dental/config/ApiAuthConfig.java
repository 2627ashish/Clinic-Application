package com.chaula.dental.config;

import com.chaula.dental.service.DoctorAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class ApiAuthConfig implements WebMvcConfigurer {
    private final DoctorAuthInterceptor doctorAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(doctorAuthInterceptor)
                .addPathPatterns("/api/appointments/**", "/api/campaigns/**")
                .excludePathPatterns("/api/appointments/ai-intake");
    }
}
