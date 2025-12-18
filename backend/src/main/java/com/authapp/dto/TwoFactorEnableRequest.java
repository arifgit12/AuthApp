package com.authapp.dto;

public class TwoFactorEnableRequest {
    private String code;
    
    public TwoFactorEnableRequest() {}
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
}
