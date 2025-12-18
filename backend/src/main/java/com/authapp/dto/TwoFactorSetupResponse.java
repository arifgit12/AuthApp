package com.authapp.dto;

import java.util.List;

public class TwoFactorSetupResponse {
    private String method;
    private String secret; // For TOTP
    private String qrCodeUrl; // For TOTP
    private List<String> backupCodes;
    private String message;
    
    public TwoFactorSetupResponse() {}
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public String getSecret() {
        return secret;
    }
    
    public void setSecret(String secret) {
        this.secret = secret;
    }
    
    public String getQrCodeUrl() {
        return qrCodeUrl;
    }
    
    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }
    
    public List<String> getBackupCodes() {
        return backupCodes;
    }
    
    public void setBackupCodes(List<String> backupCodes) {
        this.backupCodes = backupCodes;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
