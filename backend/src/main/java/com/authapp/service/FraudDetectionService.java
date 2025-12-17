package com.authapp.service;

import com.authapp.model.LoginAttempt;
import com.authapp.repository.LoginAttemptRepository;
import com.authapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FraudDetectionService {
    
    @Autowired
    private LoginAttemptRepository loginAttemptRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Value("${app.security.max-failed-attempts:5}")
    private int maxFailedAttempts;
    
    @Value("${app.security.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;
    
    @Value("${app.security.fraud-detection-window-minutes:60}")
    private int fraudDetectionWindowMinutes;
    
    @Transactional
    public void recordLoginAttempt(String username, String ipAddress, String userAgent, 
                                   boolean success, String failureReason) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setUsername(username);
        attempt.setIpAddress(ipAddress);
        attempt.setUserAgent(userAgent);
        attempt.setSuccess(success);
        attempt.setFailureReason(failureReason);
        
        // Calculate risk score
        int riskScore = calculateRiskScore(username, ipAddress);
        attempt.setRiskScore(riskScore);
        attempt.setSuspicious(riskScore > 50);
        
        loginAttemptRepository.save(attempt);
        
        // Handle account locking
        if (!success) {
            handleFailedLogin(username);
        }
    }
    
    private int calculateRiskScore(String username, String ipAddress) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(fraudDetectionWindowMinutes);
        
        long failedAttempts = loginAttemptRepository.countFailedAttempts(username, windowStart);
        long failedAttemptsByIp = loginAttemptRepository.countFailedAttemptsByIp(ipAddress, windowStart);
        
        int riskScore = 0;
        
        // Multiple failed attempts from same username
        if (failedAttempts > 3) {
            riskScore += 30;
        }
        if (failedAttempts > 5) {
            riskScore += 20;
        }
        
        // Multiple failed attempts from same IP
        if (failedAttemptsByIp > 10) {
            riskScore += 30;
        }
        
        // Check for rapid attempts
        List<LoginAttempt> recentAttempts = loginAttemptRepository
                .findByUsernameAndAttemptTimeAfter(username, LocalDateTime.now().minusMinutes(5));
        if (recentAttempts.size() > 5) {
            riskScore += 20;
        }
        
        return Math.min(riskScore, 100);
    }
    
    private void handleFailedLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            
            if (attempts >= maxFailedAttempts) {
                user.setLocked(true);
            }
            
            userRepository.save(user);
        });
    }
    
    @Transactional
    public void handleSuccessfulLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        });
    }
    
    public boolean isAccountLocked(String username) {
        return userRepository.findByUsername(username)
                .map(user -> user.isLocked())
                .orElse(false);
    }
    
    public boolean isSuspiciousActivity(String username, String ipAddress) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(fraudDetectionWindowMinutes);
        
        long failedAttempts = loginAttemptRepository.countFailedAttempts(username, windowStart);
        long failedAttemptsByIp = loginAttemptRepository.countFailedAttemptsByIp(ipAddress, windowStart);
        
        return failedAttempts > maxFailedAttempts || failedAttemptsByIp > 20;
    }
}
