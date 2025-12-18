package com.authapp.dto;

public class RecaptchaRequest {
    private String recaptchaToken;
    
    public RecaptchaRequest() {}
    
    public String getRecaptchaToken() {
        return recaptchaToken;
    }
    
    public void setRecaptchaToken(String recaptchaToken) {
        this.recaptchaToken = recaptchaToken;
    }
}
