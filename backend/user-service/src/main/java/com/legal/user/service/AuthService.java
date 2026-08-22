package com.legal.user.service;

import com.legal.user.model.Role;
import com.legal.user.model.User;
import com.legal.user.repository.RoleRepository;
import com.legal.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerUser(String fullName, String email, String phone, String password) throws Exception {
        String baseUsername = email.split("@")[0];

        // Fetch the PENDING role from DB
        Role pendingRole = roleRepository.findByName("PENDING")
                .orElseThrow(() -> new Exception("Role PENDING not found in database"));

        User user = User.builder()
                .name(fullName)
                .email(email)
                .username(baseUsername)
                .phone(phone)
                .password(passwordEncoder.encode(password))
                .status(User.UserStatus.PENDING_VERIFICATION) // RN-S-007
                .build();
        
        user.getRoles().add(pendingRole);
                
        return userRepository.save(user);
    }

    public User processOAuthLogin(String oauthEmail, String oauthFullName) throws Exception {
        Optional existingUser = userRepository.findByEmail(oauthEmail);
        
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        // RN-S-002 & RN-S-008: New OAuth User
        Role pendingRole = roleRepository.findByName("PENDING")
                .orElseThrow(() -> new Exception("Role PENDING not found in database"));

        String tempPassword = UUID.randomUUID().toString().substring(0, 8);
        String baseUsername = oauthEmail.split("@")[0];

        User newUser = User.builder()
                .name(oauthFullName)
                .email(oauthEmail)
                .username(baseUsername)
                .password(passwordEncoder.encode(tempPassword))
                .status(User.UserStatus.PENDING_VERIFICATION)
                .requiresOAuthSetup(true)
                .isEmailVerified(true) 
                .build();
        
        newUser.getRoles().add(pendingRole);
        userRepository.save(newUser);
        
        // TODO: Send email via EmailService with tempPassword
        System.out.println("Temporary password for " + oauthEmail + " is " + tempPassword);
        
        return newUser;
    }

    public User validateLogin(String email, String rawPassword, String deviceId) throws Exception {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("Invalid credentials"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new Exception("Invalid credentials");
        }

        if (user.getRequiresOAuthSetup() != null && user.getRequiresOAuthSetup()) {
            throw new Exception("OAUTH_SETUP_REQUIRED"); 
        }

        if (!user.isActive()) {
            throw new Exception("ACCOUNT_NOT_ACTIVE");
        }

        // RN-S-003: System Admin Device Validation (Utilizing your hasRole method!)
        if (user.hasRole("SYSTEM_ADMIN")) {
            if (!user.getDeviceIds().contains(deviceId)) {
                if (user.getDeviceIds().size() >= 2) {
                    throw new Exception("Device limit reached for System Admin (Max 2).");
                }
                user.getDeviceIds().add(deviceId);
            }
        }
        
        user.setLastLogin(LocalDateTime.now());
        return userRepository.save(user);
    }

    public void completeOAuthSetup(String email, String oldTempPassword, String newPassword, String altEmail, String phone) throws Exception {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("User not found"));

        if (!passwordEncoder.matches(oldTempPassword, user.getPassword())) {
            throw new Exception("Invalid temporary password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setAltEmail(altEmail);
        user.setPhone(phone);
        user.setRequiresOAuthSetup(false);
        userRepository.save(user);
    }
}
