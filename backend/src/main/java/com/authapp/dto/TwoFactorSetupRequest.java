package com.authapp.dto;

public class TwoFactorSetupRequest {
    private String method; // TOTP, SMS, EMAIL
    private String phoneNumber; // Required for SMS
    
    public TwoFactorSetupRequest() {}
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
