package com.authapp.security.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationStrategy implements AuthenticationStrategy {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Override
    public Authentication authenticate(String username, String password) {
        // For JWT, we still authenticate with username/password first
        // The JWT token is generated after successful authentication
        return authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(username, password)
        );
    }
    
    @Override
    public String getAuthMethodName() {
        return "JWT";
    }
    
    @Override
    public boolean supports(String authMethod) {
        return "JWT".equalsIgnoreCase(authMethod);
    }
}
