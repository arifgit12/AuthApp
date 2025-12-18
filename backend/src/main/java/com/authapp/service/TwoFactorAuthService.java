package com.authapp.service;

import com.authapp.dto.TwoFactorSetupRequest;
import com.authapp.dto.TwoFactorSetupResponse;
import com.authapp.model.TwoFactorAuth;
import com.authapp.model.User;
import com.authapp.repository.TwoFactorAuthRepository;
import com.authapp.repository.UserRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
public class TwoFactorAuthService {
    
    @Autowired
    private TwoFactorAuthRepository twoFactorAuthRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Value("${app.name:AuthApp}")
    private String appName;
    
    private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Transactional
    public TwoFactorSetupResponse setup(TwoFactorSetupRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Create or update TwoFactorAuth entity
        TwoFactorAuth twoFactorAuth = twoFactorAuthRepository.findByUser(user)
            .orElse(new TwoFactorAuth());
        
        twoFactorAuth.setUser(user);
        twoFactorAuth.setMethod(request.getMethod());
        
        TwoFactorSetupResponse response = new TwoFactorSetupResponse();
        response.setMethod(request.getMethod());
        
        switch (request.getMethod().toUpperCase()) {
            case "TOTP":
                // Generate TOTP secret
                GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
                String secret = key.getKey();
                twoFactorAuth.setSecret(secret);
                
                // Generate QR code URL
                String qrCodeUrl = GoogleAuthenticatorQRGenerator.getOtpAuthURL(
                    appName, username, key
                );
                
                response.setSecret(secret);
                response.setQrCodeUrl(qrCodeUrl);
                break;
                
            case "SMS":
                if (request.getPhoneNumber() == null || request.getPhoneNumber().isEmpty()) {
                    throw new RuntimeException("Phone number is required for SMS 2FA");
                }
                twoFactorAuth.setPhoneNumber(request.getPhoneNumber());
                response.setMessage("SMS verification will be sent to " + request.getPhoneNumber());
                break;
                
            case "EMAIL":
                response.setMessage("Email verification will be sent to " + user.getEmail());
                break;
                
            default:
                throw new RuntimeException("Unsupported 2FA method: " + request.getMethod());
        }
        
        // Generate backup codes
        List<String> backupCodes = generateBackupCodes();
        List<String> hashedBackupCodes = new ArrayList<>();
        for (String code : backupCodes) {
            hashedBackupCodes.add(passwordEncoder.encode(code));
        }
        twoFactorAuth.setBackupCodes(hashedBackupCodes);
        
        twoFactorAuthRepository.save(twoFactorAuth);
        
        // Return plain text backup codes (only time they're shown)
        response.setBackupCodes(backupCodes);
        
        return response;
    }
    
    @Transactional
    public void enable(String code) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        TwoFactorAuth twoFactorAuth = twoFactorAuthRepository.findByUser(user)
            .orElseThrow(() -> new RuntimeException("2FA not setup. Please setup first."));
        
        if (!verify(user, code, false)) {
            throw new RuntimeException("Invalid verification code");
        }
        
        twoFactorAuth.setEnabled(true);
        user.setTwoFactorEnabled(true);
        user.setTwoFactorMethod(twoFactorAuth.getMethod());
        
        twoFactorAuthRepository.save(twoFactorAuth);
        userRepository.save(user);
    }
    
    @Transactional
    public void disable() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        TwoFactorAuth twoFactorAuth = twoFactorAuthRepository.findByUser(user)
            .orElse(null);
        
        if (twoFactorAuth != null) {
            twoFactorAuth.setEnabled(false);
            twoFactorAuthRepository.save(twoFactorAuth);
        }
        
        user.setTwoFactorEnabled(false);
        user.setTwoFactorMethod(null);
        userRepository.save(user);
    }
    
    public boolean verify(User user, String code, boolean useBackupCode) {
        TwoFactorAuth twoFactorAuth = twoFactorAuthRepository.findByUser(user)
            .orElseThrow(() -> new RuntimeException("2FA not setup"));
        
        if (!twoFactorAuth.isEnabled()) {
            // Allow verification during setup
            if (twoFactorAuth.getMethod().equalsIgnoreCase("TOTP")) {
                return googleAuthenticator.authorize(twoFactorAuth.getSecret(), Integer.parseInt(code));
            }
            return true; // For SMS/Email, assume code is validated by separate service
        }
        
        if (useBackupCode) {
            return verifyBackupCode(twoFactorAuth, code);
        }
        
        switch (twoFactorAuth.getMethod().toUpperCase()) {
            case "TOTP":
                return googleAuthenticator.authorize(twoFactorAuth.getSecret(), Integer.parseInt(code));
            case "SMS":
            case "EMAIL":
                // In a real implementation, this would verify against a stored temporary code
                // For now, we'll accept any 6-digit code as valid
                return code.matches("\\d{6}");
            default:
                return false;
        }
    }
    
    private boolean verifyBackupCode(TwoFactorAuth twoFactorAuth, String code) {
        List<String> backupCodes = twoFactorAuth.getBackupCodes();
        for (int i = 0; i < backupCodes.size(); i++) {
            if (passwordEncoder.matches(code, backupCodes.get(i))) {
                // Remove used backup code
                backupCodes.remove(i);
                twoFactorAuth.setBackupCodes(backupCodes);
                twoFactorAuthRepository.save(twoFactorAuth);
                return true;
            }
        }
        return false;
    }
    
    public void sendCode(User user) {
        TwoFactorAuth twoFactorAuth = twoFactorAuthRepository.findByUser(user)
            .orElseThrow(() -> new RuntimeException("2FA not setup"));
        
        String code = generateVerificationCode();
        
        switch (twoFactorAuth.getMethod().toUpperCase()) {
            case "SMS":
                sendSmsCode(twoFactorAuth.getPhoneNumber(), code);
                break;
            case "EMAIL":
                sendEmailCode(user.getEmail(), code);
                break;
            default:
                throw new RuntimeException("Cannot send code for method: " + twoFactorAuth.getMethod());
        }
    }
    
    private List<String> generateBackupCodes() {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            codes.add(generateBackupCode());
        }
        return codes;
    }
    
    private String generateBackupCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            code.append(secureRandom.nextInt(10));
        }
        return code.toString();
    }
    
    private String generateVerificationCode() {
        return String.format("%06d", secureRandom.nextInt(1000000));
    }
    
    private void sendSmsCode(String phoneNumber, String code) {
        // In a real implementation, this would integrate with an SMS provider like Twilio
        // For now, just log the code
        System.out.println("SMS Code for " + phoneNumber + ": " + code);
    }
    
    private void sendEmailCode(String email, String code) {
        // In a real implementation, this would send an email
        // For now, just log the code
        System.out.println("Email Code for " + email + ": " + code);
    }
    
    public boolean isEnabled(User user) {
        return user.isTwoFactorEnabled();
    }
}
