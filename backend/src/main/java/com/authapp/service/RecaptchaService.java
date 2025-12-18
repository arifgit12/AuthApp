package com.authapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class RecaptchaService {
    
    @Value("${app.recaptcha.secret:}")
    private String recaptchaSecret;
    
    @Value("${app.recaptcha.enabled:false}")
    private boolean recaptchaEnabled;
    
    private final WebClient webClient = WebClient.builder().build();
    
    public boolean verify(String recaptchaToken) {
        if (!recaptchaEnabled || recaptchaSecret == null || recaptchaSecret.isEmpty()) {
            // If reCAPTCHA is not configured, skip verification
            return true;
        }
        
        if (recaptchaToken == null || recaptchaToken.isEmpty()) {
            return false;
        }
        
        try {
            Map<String, Object> response = webClient.post()
                .uri("https://www.google.com/recaptcha/api/siteverify")
                .bodyValue(Map.of(
                    "secret", recaptchaSecret,
                    "response", recaptchaToken
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            return response != null && Boolean.TRUE.equals(response.get("success"));
        } catch (Exception e) {
            System.err.println("reCAPTCHA verification failed: " + e.getMessage());
            return false;
        }
    }
}
