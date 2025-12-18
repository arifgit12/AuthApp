package com.authapp.dto;

public class TwoFactorVerifyRequest {
    private String username;
    private String code;
    private boolean useBackupCode;
    
    public TwoFactorVerifyRequest() {}
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public boolean isUseBackupCode() {
        return useBackupCode;
    }
    
    public void setUseBackupCode(boolean useBackupCode) {
        this.useBackupCode = useBackupCode;
    }
}
