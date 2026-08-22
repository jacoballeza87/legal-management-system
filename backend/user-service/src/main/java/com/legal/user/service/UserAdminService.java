package com.legal.user.service;

import com.legal.user.model.Role;
import com.legal.user.model.User;
import com.legal.user.repository.RoleRepository;
import com.legal.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserAdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    public User assignRoleToUser(Long userId, String roleName) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found"));

        Role newRole = roleRepository.findByName(roleName)
                .orElseThrow(() -> new Exception("Role not found"));

        // RN-S-004: Validate System Admin limits before assigning
        if ("SYSTEM_ADMIN".equals(roleName)) {
            long currentSystemAdmins = userRepository.countByRoleName("SYSTEM_ADMIN");
            if (currentSystemAdmins >= 5) {
                throw new Exception("Maximum limit of 5 System Administrators reached.");
            }
        }

        // Add the new role
        user.getRoles().add(newRole);
        return userRepository.save(user);
    }
}
