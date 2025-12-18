# Two-Factor Authentication Implementation Guide

This document provides guidance on using the two-factor authentication features added to AuthApp.

## Overview

The AuthApp now supports multiple two-factor authentication methods:

1. **TOTP (Time-based One-Time Password)** - Use authenticator apps like Google Authenticator, Authy, or Microsoft Authenticator
2. **SMS** - Receive verification codes via text message (requires SMS provider configuration)
3. **EMAIL** - Receive verification codes via email (requires email server configuration)

All methods include backup codes for account recovery.

## Backend Implementation

### New Endpoints

#### Setup 2FA
```
POST /api/auth/2fa/setup
Content-Type: application/json

{
  "method": "TOTP|SMS|EMAIL",
  "phoneNumber": "+1234567890"  // Required only for SMS
}
```

Response:
```json
{
  "method": "TOTP",
  "secret": "JBSWY3DPEHPK3PXP",  // For TOTP only
  "qrCodeUrl": "otpauth://totp/...",  // For TOTP only
  "backupCodes": ["12345678", "87654321", ...],
  "message": "Setup message"
}
```

#### Enable 2FA
```
POST /api/auth/2fa/enable
Content-Type: application/json

{
  "code": "123456"
}
```

#### Disable 2FA
```
POST /api/auth/2fa/disable
```

#### Send Verification Code (SMS/Email only)
```
POST /api/auth/2fa/send-code
```

#### Login with 2FA
```
POST /api/auth/login
Content-Type: application/json

{
  "username": "user",
  "password": "password",
  "authMethod": "JWT",
  "twoFactorCode": "123456"  // Provide after initial login attempt
}
```

### Configuration

Add these properties to `application.properties`:

```properties
# Two-Factor Authentication
app.name=AuthApp
app.2fa.enabled=true

# Google reCAPTCHA (Optional - leave empty to disable)
app.recaptcha.enabled=false
app.recaptcha.site-key=your-site-key
app.recaptcha.secret=your-secret-key

# Email Configuration (for Email 2FA)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# SMS Configuration (for SMS 2FA - would integrate with Twilio or similar)
app.sms.enabled=false
app.sms.provider=twilio
app.sms.api-key=your-api-key
app.sms.api-secret=your-api-secret
```

## Frontend Implementation

### Login Flow

1. User enters username and password
2. If 2FA is enabled:
   - Backend responds with `twoFactorRequired: true`
   - Frontend displays 2FA code input
   - User enters 6-digit code (or 8-digit backup code)
   - Submit code with credentials
3. If 2FA is not enabled or code is valid:
   - User is logged in successfully

### Setup Flow (Dashboard)

1. Click "Setup 2FA" button on dashboard
2. Choose authentication method (TOTP, SMS, or EMAIL)
3. For TOTP: Scan QR code with authenticator app
4. For SMS: Enter phone number
5. For EMAIL: Uses registered email address
6. Save backup codes (displayed once)
7. Enter verification code to enable 2FA

## Security Features

### TOTP (Authenticator Apps)
- Uses Google Authenticator compatible library
- Generates QR codes for easy setup
- Most secure method (no external dependencies)

### SMS/Email
- Generates random 6-digit codes
- Codes are logged to console (for demo purposes)
- Production implementation should integrate with:
  - SMS: Twilio, AWS SNS, or similar
  - Email: SMTP server configuration

### Backup Codes
- 10 backup codes generated during setup
- Each code is 8 digits long
- Codes are hashed and stored securely
- Can be used once each
- Should be stored safely by the user

### reCAPTCHA Support
- Infrastructure ready for Google reCAPTCHA v2 or v3
- Add site key and secret to application.properties
- Frontend will display reCAPTCHA widget when enabled

## Testing

### Manual Testing Steps

1. Start backend: `cd backend && mvn spring-boot:run`
2. Start frontend: `cd frontend && npm start`
3. Navigate to http://localhost:4200
4. Log in with default credentials:
   - Username: `admin` / Password: `admin123`
   - Username: `user` / Password: `user123`
5. Navigate to Dashboard
6. Click "Setup 2FA"
7. Choose a method and follow setup flow
8. Log out and log in again to test 2FA verification

### Integration with External Services

For production use, integrate with:

**SMS Provider (Twilio example):**
```java
// In TwoFactorAuthService.sendSmsCode()
TwilioRestClient client = new TwilioRestClient(apiKey, apiSecret);
Message.creator(
    new PhoneNumber(phoneNumber),
    new PhoneNumber(fromNumber),
    "Your verification code is: " + code
).create();
```

**Email Service:**
```java
// Already configured - just update application.properties
// with your SMTP server details
```

## Database Schema

### New Tables

**two_factor_auth**
- id (PK)
- user_id (FK to users, unique)
- enabled (boolean)
- method (TOTP, SMS, EMAIL)
- secret (for TOTP)
- phone_number (for SMS)
- created_at
- updated_at

**backup_codes**
- two_factor_auth_id (FK)
- code (hashed)

### Updated Tables

**users**
- Added: two_factor_enabled (boolean)
- Added: two_factor_method (varchar)

## Dependencies Added

### Backend (Maven)
```xml
<!-- Two-Factor Authentication -->
<dependency>
    <groupId>com.warrenstrange</groupId>
    <artifactId>googleauth</artifactId>
    <version>1.5.0</version>
</dependency>

<!-- Google reCAPTCHA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<!-- Email Support -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

## Future Enhancements

- WebAuthn/FIDO2 support
- Biometric authentication
- Remember trusted devices
- Time-based code expiration for SMS/Email
- Rate limiting for 2FA attempts
- Admin panel for managing user 2FA settings
- Audit logs for 2FA events
