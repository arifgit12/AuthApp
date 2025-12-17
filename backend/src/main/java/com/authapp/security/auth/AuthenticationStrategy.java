package com.authapp.security.auth;

import org.springframework.security.core.Authentication;

public interface AuthenticationStrategy {
    Authentication authenticate(String username, String password);
    String getAuthMethodName();
    boolean supports(String authMethod);
}
