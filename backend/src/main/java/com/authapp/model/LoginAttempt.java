package com.authapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginAttempt {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String username;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(nullable = false)
    private boolean success;
    
    @Column(name = "failure_reason")
    private String failureReason;
    
    @Column(name = "attempt_time")
    private LocalDateTime attemptTime;
    
    @Column(name = "is_suspicious")
    private boolean isSuspicious = false;
    
    @Column(name = "risk_score")
    private Integer riskScore = 0;
    
    @PrePersist
    protected void onCreate() {
        attemptTime = LocalDateTime.now();
    }
}
