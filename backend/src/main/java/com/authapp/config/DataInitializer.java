package com.authapp.config;

import com.authapp.model.Privilege;
import com.authapp.model.Role;
import com.authapp.model.User;
import com.authapp.repository.PrivilegeRepository;
import com.authapp.repository.RoleRepository;
import com.authapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PrivilegeRepository privilegeRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Create privileges
        Privilege readPrivilege = createPrivilegeIfNotFound("READ_PRIVILEGE", "Read access", "ALL", "READ");
        Privilege writePrivilege = createPrivilegeIfNotFound("WRITE_PRIVILEGE", "Write access", "ALL", "WRITE");
        Privilege deletePrivilege = createPrivilegeIfNotFound("DELETE_PRIVILEGE", "Delete access", "ALL", "DELETE");
        Privilege adminPrivilege = createPrivilegeIfNotFound("ADMIN_PRIVILEGE", "Admin access", "ALL", "ADMIN");
        
        // Create roles
        Set<Privilege> userPrivileges = new HashSet<>();
        userPrivileges.add(readPrivilege);
        Role userRole = createRoleIfNotFound("USER", "Standard user role", userPrivileges);
        
        Set<Privilege> moderatorPrivileges = new HashSet<>();
        moderatorPrivileges.add(readPrivilege);
        moderatorPrivileges.add(writePrivilege);
        Role moderatorRole = createRoleIfNotFound("MODERATOR", "Moderator role", moderatorPrivileges);
        
        Set<Privilege> adminPrivileges = new HashSet<>();
        adminPrivileges.add(readPrivilege);
        adminPrivileges.add(writePrivilege);
        adminPrivileges.add(deletePrivilege);
        adminPrivileges.add(adminPrivilege);
        Role adminRole = createRoleIfNotFound("ADMIN", "Administrator role", adminPrivileges);
        
        // Create default admin user
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@authapp.com");
            admin.setFullName("System Administrator");
            admin.setActive(true);
            
            Set<Role> roles = new HashSet<>();
            roles.add(adminRole);
            admin.setRoles(roles);
            
            userRepository.save(admin);
        }
        
        // Create default user
        if (!userRepository.existsByUsername("user")) {
            User user = new User();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setEmail("user@authapp.com");
            user.setFullName("Standard User");
            user.setActive(true);
            
            Set<Role> roles = new HashSet<>();
            roles.add(userRole);
            user.setRoles(roles);
            
            userRepository.save(user);
        }
    }
    
    @Transactional
    Privilege createPrivilegeIfNotFound(String name, String description, String resourceType, String actionType) {
        return privilegeRepository.findByName(name).orElseGet(() -> {
            Privilege privilege = new Privilege();
            privilege.setName(name);
            privilege.setDescription(description);
            privilege.setResourceType(resourceType);
            privilege.setActionType(actionType);
            return privilegeRepository.save(privilege);
        });
    }
    
    @Transactional
    Role createRoleIfNotFound(String name, String description, Set<Privilege> privileges) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role role = new Role();
            role.setName(name);
            role.setDescription(description);
            role.setPrivileges(privileges);
            return roleRepository.save(role);
        });
    }
}
