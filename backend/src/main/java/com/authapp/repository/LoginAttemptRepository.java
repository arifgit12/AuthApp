package com.authapp.repository;

import com.authapp.model.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
    
    List<LoginAttempt> findByUsernameAndAttemptTimeAfter(String username, LocalDateTime time);
    
    List<LoginAttempt> findByIpAddressAndAttemptTimeAfter(String ipAddress, LocalDateTime time);
    
    @Query("SELECT COUNT(l) FROM LoginAttempt l WHERE l.username = ?1 AND l.success = false AND l.attemptTime > ?2")
    long countFailedAttempts(String username, LocalDateTime time);
    
    @Query("SELECT COUNT(l) FROM LoginAttempt l WHERE l.ipAddress = ?1 AND l.success = false AND l.attemptTime > ?2")
    long countFailedAttemptsByIp(String ipAddress, LocalDateTime time);
}
