package com.authapp.controller;

import com.authapp.dto.*;
import com.authapp.model.User;
import com.authapp.repository.UserRepository;
import com.authapp.service.AuthenticationService;
import com.authapp.service.TwoFactorAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @Autowired
    private TwoFactorAuthService twoFactorAuthService;
    
    @Autowired
    private UserRepository userRepository;
    
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
                                              HttpServletRequest request) {
        try {
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            LoginResponse response = authenticationService.authenticate(loginRequest, ipAddress, userAgent);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            authenticationService.register(registerRequest);
            return ResponseEntity.ok("User registered successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        return ResponseEntity.ok("User logged out successfully");
    }
    
    @PostMapping("/2fa/setup")
    public ResponseEntity<?> setup2FA(@Valid @RequestBody TwoFactorSetupRequest request) {
        try {
            TwoFactorSetupResponse response = twoFactorAuthService.setup(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/2fa/enable")
    public ResponseEntity<?> enable2FA(@Valid @RequestBody TwoFactorEnableRequest request) {
        try {
            twoFactorAuthService.enable(request.getCode());
            return ResponseEntity.ok("Two-factor authentication enabled successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/2fa/disable")
    public ResponseEntity<?> disable2FA() {
        try {
            twoFactorAuthService.disable();
            return ResponseEntity.ok("Two-factor authentication disabled successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/2fa/verify")
    public ResponseEntity<?> verify2FA(@Valid @RequestBody TwoFactorVerifyRequest request,
                                       HttpServletRequest httpRequest) {
        try {
            User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            boolean isValid = twoFactorAuthService.verify(user, request.getCode(), request.isUseBackupCode());
            
            if (isValid) {
                return ResponseEntity.ok("2FA verification successful");
            } else {
                return ResponseEntity.badRequest().body("Invalid 2FA code");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/2fa/send-code")
    public ResponseEntity<?> sendCode() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            twoFactorAuthService.sendCode(user);
            return ResponseEntity.ok("Verification code sent successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
