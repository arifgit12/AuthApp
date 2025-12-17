# Security Configuration Guide

## Overview

This document outlines the security features and configurations for the AuthApp application.

## Authentication Security

### JWT Configuration

**Key Security Points:**
- JWT tokens expire after 24 hours (configurable)
- Tokens are signed using HS512 algorithm
- Secret key should be at least 512 bits (64 characters)

**Production Configuration:**
```properties
# Generate a strong secret key (512 bits minimum)
app.jwt.secret=<generate-using-openssl-rand-base64-64>

# Token expiration (in milliseconds)
# 3600000 = 1 hour
# 86400000 = 24 hours
# 604800000 = 7 days
app.jwt.expiration=86400000
```

**Generate Secure JWT Secret:**
```bash
openssl rand -base64 64
```

### Password Security

**BCrypt Configuration:**
- Password hashing strength: 10 rounds (BCrypt default)
- Passwords are salted automatically
- Never store plain text passwords

**Password Requirements:**
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one number
- Special characters recommended

**Implementation:**
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

## Fraud Detection

### Login Attempt Monitoring

**Configuration:**
```properties
# Maximum failed login attempts before locking
app.security.max-failed-attempts=5

# Account lockout duration in minutes
app.security.lockout-duration-minutes=30

# Time window for fraud detection in minutes
app.security.fraud-detection-window-minutes=60
```

### Risk Score Calculation

Risk scores are calculated based on:
- Failed login attempts from same username (30 points after 3 attempts, +20 after 5)
- Failed login attempts from same IP (30 points after 10 attempts)
- Rapid successive attempts (20 points for >5 attempts in 5 minutes)

**Risk Levels:**
- 0-30: Low risk
- 31-50: Medium risk
- 51-100: High risk (suspicious activity)

### Automatic Account Locking

Accounts are automatically locked when:
- Failed login attempts exceed configured threshold
- High-risk score detected (>50)
- Multiple suspicious activities detected

**Unlock Procedure:**
1. Admin manual unlock through database
2. Automatic unlock after lockout duration
3. Password reset process

## CORS Configuration

**Security Configuration:**
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    
    // Only allow specific origins in production
    configuration.setAllowedOrigins(Arrays.asList(
        "https://your-domain.com",
        "https://app.your-domain.com"
    ));
    
    // Allowed HTTP methods
    configuration.setAllowedMethods(Arrays.asList(
        "GET", "POST", "PUT", "DELETE", "OPTIONS"
    ));
    
    // Allowed headers
    configuration.setAllowedHeaders(Arrays.asList("*"));
    
    // Expose headers
    configuration.setExposedHeaders(Arrays.asList("Authorization"));
    
    // Allow credentials
    configuration.setAllowCredentials(true);
    
    // Cache preflight requests for 1 hour
    configuration.setMaxAge(3600L);
    
    return source;
}
```

**Production Best Practices:**
- Never use wildcard (*) for allowed origins in production
- Specify exact domains
- Enable credentials only if needed
- Use HTTPS for all allowed origins

## CSRF Protection

**Configuration:**
- CSRF protection is disabled for stateless JWT APIs (this is a common and acceptable practice)
- JWT tokens in Authorization headers are not vulnerable to CSRF attacks
- Browsers don't automatically attach Authorization headers like they do with cookies
- Enable CSRF for session-based authentication if using cookies

**Why CSRF is disabled for JWT:**
1. JWT tokens are stored in sessionStorage/localStorage (not cookies)
2. Tokens are manually added to Authorization header
3. Browsers don't automatically send Authorization headers
4. CSRF attacks rely on automatic cookie transmission

```java
// For stateless JWT APIs (current implementation)
http.csrf(csrf -> csrf.disable())

// For session-based authentication with cookies
http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
```

**Note:** If you decide to use cookies for token storage instead of sessionStorage, you MUST enable CSRF protection.

## Session Management

**Stateless Configuration:**
```java
http.sessionManagement(session -> 
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
```

**Benefits:**
- No server-side session storage
- Horizontal scaling friendly
- Reduced memory footprint
- Better for microservices architecture

## HTTPS/TLS Configuration

### Spring Boot HTTPS

**application.properties:**
```properties
server.port=8443
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=tomcat
```

### Generate Self-Signed Certificate (Development)

```bash
keytool -genkeypair -alias tomcat -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore keystore.p12 -validity 3650 \
  -storepass changeit
```

### Production Certificate

Use Let's Encrypt or commercial CA:
```bash
# Let's Encrypt with Certbot
certbot certonly --standalone -d your-domain.com
```

## Security Headers

**Implemented Headers:**

1. **X-Content-Type-Options**: Prevents MIME type sniffing
```java
headers().contentTypeOptions()
```

2. **X-Frame-Options**: Prevents clickjacking
```java
headers().frameOptions().deny()
```

3. **X-XSS-Protection**: Enables XSS filter
```java
headers().xssProtection()
```

4. **Strict-Transport-Security**: Forces HTTPS
```java
headers().httpStrictTransportSecurity()
```

5. **Content-Security-Policy**: Prevents XSS and injection
```java
headers().contentSecurityPolicy("default-src 'self'")
```

## Input Validation

### Backend Validation

**Using Bean Validation:**
```java
public class LoginRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50)
    private String username;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100)
    private String password;
}
```

### Frontend Validation

**Angular Forms:**
```typescript
this.loginForm = this.fb.group({
  username: ['', [Validators.required, Validators.minLength(3)]],
  password: ['', [Validators.required, Validators.minLength(8)]]
});
```

## SQL Injection Prevention

**Using JPA/Hibernate:**
- Always use parameterized queries
- Never concatenate user input in queries
- Use JPA criteria API or JPQL

```java
// GOOD - Parameterized
@Query("SELECT u FROM User u WHERE u.username = ?1")
Optional<User> findByUsername(String username);

// BAD - Never do this
// "SELECT * FROM users WHERE username = '" + username + "'"
```

## XSS Prevention

### Backend
- Sanitize user input
- Use Content-Security-Policy headers
- Escape output

### Frontend
- Angular automatically escapes HTML
- Avoid using `innerHTML` with user input
- Use `DomSanitizer` when necessary

```typescript
// Angular automatically escapes
{{ userInput }}  // Safe

// Avoid
[innerHTML]="userInput"  // Unsafe without sanitization
```

## API Rate Limiting

**Recommended Implementation:**

Using Spring Security:
```java
@Bean
public RateLimiter rateLimiter() {
    return RateLimiter.of("backendRateLimiter", 
        RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .limitForPeriod(100)
            .build());
}
```

Or use external tools:
- Redis with Spring Data Redis
- API Gateway (Kong, AWS API Gateway)
- Nginx rate limiting

## Audit Logging

**Security Events Logged:**
- Login attempts (success/failure)
- Password changes
- Account locks
- Role/privilege changes
- Suspicious activities

**Implementation:**
```java
private void logSecurityEvent(String event, String username, String details) {
    logger.info("Security Event: {} - User: {} - Details: {}", 
        event, username, details);
}
```

**Log Format:**
```
2024-01-15 10:30:00 INFO  Security Event: LOGIN_FAILED - User: johndoe - Details: Invalid password - IP: 192.168.1.100
```

## Database Security

### Connection Security

**Production Configuration:**
```properties
# Use SSL for database connection
spring.datasource.url=jdbc:postgresql://db-host:5432/authdb?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory

# Strong passwords
spring.datasource.username=authapp_user
spring.datasource.password=<strong-password>

# Connection pool
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
```

### Database User Permissions

```sql
-- Create dedicated database user
CREATE USER authapp_user WITH PASSWORD 'strong-password';

-- Grant minimal required permissions
GRANT CONNECT ON DATABASE authdb TO authapp_user;
GRANT USAGE ON SCHEMA public TO authapp_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO authapp_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO authapp_user;
```

## LDAP Security

**Secure Configuration:**
```properties
# Use LDAPS (LDAP over SSL)
spring.ldap.urls=ldaps://ldap-server:636

# Connection timeout
spring.ldap.base=dc=example,dc=com

# Use service account with minimal permissions
spring.ldap.username=cn=readonly,dc=example,dc=com
spring.ldap.password=<strong-password>
```

## Keycloak Security

**Client Configuration:**
```properties
# Use HTTPS
keycloak.auth-server-url=https://keycloak.your-domain.com/auth

# Confidential client
keycloak.public-client=false
keycloak.credentials.secret=<strong-client-secret>

# Require SSL
keycloak.ssl-required=all

# Enable token validation
keycloak.bearer-only=true
keycloak.verify-token-audience=true
```

## Secrets Management

### Development
- Use environment variables
- Use `application-dev.properties`
- Never commit secrets to Git

### Production
- Use secret management service:
  - AWS Secrets Manager
  - Azure Key Vault
  - HashiCorp Vault
  - Kubernetes Secrets

**Example using Environment Variables:**
```properties
app.jwt.secret=${JWT_SECRET}
spring.datasource.password=${DB_PASSWORD}
keycloak.credentials.secret=${KEYCLOAK_SECRET}
```

## Security Checklist

### Pre-Production
- [ ] Change all default passwords
- [ ] Generate strong JWT secret
- [ ] Configure HTTPS/TLS
- [ ] Set up proper CORS policy
- [ ] Enable security headers
- [ ] Configure rate limiting
- [ ] Set up audit logging
- [ ] Review and test authentication flows
- [ ] Test fraud detection
- [ ] Verify input validation
- [ ] Check for SQL injection vulnerabilities
- [ ] Test XSS prevention
- [ ] Configure secure session management
- [ ] Set up database connection security
- [ ] Configure proper file permissions
- [ ] Enable firewall rules
- [ ] Set up monitoring and alerts

### Post-Production
- [ ] Monitor security logs
- [ ] Review failed login attempts
- [ ] Check for suspicious activities
- [ ] Update dependencies regularly
- [ ] Perform security audits
- [ ] Test disaster recovery
- [ ] Review access logs
- [ ] Update documentation

## Incident Response

### Security Incident Procedure

1. **Detect**: Monitor logs and alerts
2. **Contain**: Lock affected accounts, block suspicious IPs
3. **Investigate**: Review logs, identify attack vector
4. **Remediate**: Patch vulnerabilities, update configurations
5. **Document**: Record incident details and lessons learned

### Emergency Contacts

- Security Team: security@example.com
- On-Call: +1-XXX-XXX-XXXX
- Management: management@example.com

## Compliance

### GDPR Compliance
- User data encryption
- Right to be forgotten
- Data breach notification
- Privacy by design

### OWASP Top 10 Coverage
- [x] Injection (SQL, XSS)
- [x] Broken Authentication
- [x] Sensitive Data Exposure
- [x] XML External Entities (XXE)
- [x] Broken Access Control
- [x] Security Misconfiguration
- [x] Cross-Site Scripting (XSS)
- [x] Insecure Deserialization
- [x] Using Components with Known Vulnerabilities
- [x] Insufficient Logging & Monitoring

## Resources

- [OWASP Cheat Sheet Series](https://cheatsheetseries.owasp.org/)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [NIST Password Guidelines](https://pages.nist.gov/800-63-3/)

## Support

For security concerns:
- Email: security@example.com
- Create a private security issue in GitHub
- Contact: Admin team directly
