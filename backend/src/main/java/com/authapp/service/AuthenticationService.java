package com.authapp.service;

import com.authapp.dto.LoginRequest;
import com.authapp.dto.LoginResponse;
import com.authapp.dto.RegisterRequest;
import com.authapp.model.Privilege;
import com.authapp.model.Role;
import com.authapp.model.User;
import com.authapp.repository.RoleRepository;
import com.authapp.repository.UserRepository;
import com.authapp.security.auth.AuthenticationStrategy;
import com.authapp.security.jwt.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthenticationService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private FraudDetectionService fraudDetectionService;
    
    @Autowired
    private List<AuthenticationStrategy> authenticationStrategies;
    
    @Transactional
    public LoginResponse authenticate(LoginRequest loginRequest, String ipAddress, String userAgent) {
        String authMethod = loginRequest.getAuthMethod() != null ? 
            loginRequest.getAuthMethod().toUpperCase() : "JWT";
        
        // Check for suspicious activity
        if (fraudDetectionService.isSuspiciousActivity(loginRequest.getUsername(), ipAddress)) {
            fraudDetectionService.recordLoginAttempt(loginRequest.getUsername(), ipAddress, 
                userAgent, false, "Suspicious activity detected");
            throw new RuntimeException("Account temporarily locked due to suspicious activity");
        }
        
        // Check if account is locked
        if (fraudDetectionService.isAccountLocked(loginRequest.getUsername())) {
            fraudDetectionService.recordLoginAttempt(loginRequest.getUsername(), ipAddress, 
                userAgent, false, "Account locked");
            throw new RuntimeException("Account is locked. Please contact administrator.");
        }
        
        try {
            // Find appropriate authentication strategy
            AuthenticationStrategy strategy = authenticationStrategies.stream()
                .filter(s -> s.supports(authMethod))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unsupported authentication method: " + authMethod));
            
            // Authenticate using the selected strategy
            Authentication authentication = strategy.authenticate(
                loginRequest.getUsername(), 
                loginRequest.getPassword()
            );
            
            // Generate JWT token
            String jwt = jwtUtils.generateJwtToken(authentication);
            
            // Get user details
            User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Record successful login
            fraudDetectionService.recordLoginAttempt(loginRequest.getUsername(), ipAddress, 
                userAgent, true, null);
            fraudDetectionService.handleSuccessfulLogin(loginRequest.getUsername());
            
            // Prepare response
            Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
            
            Set<String> privileges = user.getRoles().stream()
                .flatMap(role -> role.getPrivileges().stream())
                .map(Privilege::getName)
                .collect(Collectors.toSet());
            
            LoginResponse response = new LoginResponse();
            response.setToken(jwt);
            response.setUsername(user.getUsername());
            response.setEmail(user.getEmail());
            response.setRoles(roles);
            response.setPrivileges(privileges);
            response.setAuthMethod(authMethod);
            
            return response;
            
        } catch (Exception e) {
            // Record failed login
            fraudDetectionService.recordLoginAttempt(loginRequest.getUsername(), ipAddress, 
                userAgent, false, e.getMessage());
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }
    
    @Transactional
    public void register(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEmail(registerRequest.getEmail());
        user.setFullName(registerRequest.getFullName());
        user.setActive(true);
        
        // Assign default role
        Role userRole = roleRepository.findByName("USER")
            .orElseGet(() -> {
                Role newRole = new Role();
                newRole.setName("USER");
                newRole.setDescription("Default user role");
                return roleRepository.save(newRole);
            });
        
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);
        
        userRepository.save(user);
    }
}
