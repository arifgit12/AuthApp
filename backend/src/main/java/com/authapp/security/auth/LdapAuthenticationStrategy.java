package com.authapp.security.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.stereotype.Component;
import org.springframework.context.ApplicationContext;

@Component
public class LdapAuthenticationStrategy implements AuthenticationStrategy {
    
    @Autowired(required = false)
    private LdapAuthenticationProvider ldapAuthenticationProvider;
    
    @Value("${app.ldap.enabled:false}")
    private boolean ldapEnabled;
    
    @Override
    public Authentication authenticate(String username, String password) {
        if (!ldapEnabled || ldapAuthenticationProvider == null) {
            throw new UnsupportedOperationException("LDAP authentication is not configured");
        }
        
        return ldapAuthenticationProvider.authenticate(
            new UsernamePasswordAuthenticationToken(username, password)
        );
    }
    
    @Override
    public String getAuthMethodName() {
        return "LDAP";
    }
    
    @Override
    public boolean supports(String authMethod) {
        return "LDAP".equalsIgnoreCase(authMethod) && ldapEnabled;
    }
}
