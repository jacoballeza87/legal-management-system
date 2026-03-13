package com.legal.user.service;

import com.legal.user.dto.*;
import com.legal.user.mapper.UserMapper;
import com.legal.user.model.*;
import com.legal.user.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public List<RoleDTO> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoleDTO getRoleById(Long id) {
        return roleRepository.findById(id)
                .map(userMapper::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + id));
    }

    @Transactional
    public RoleDTO createRole(CreateRoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("El rol ya existe: " + request.getName());
        }

        Set<Permission> permissions = Set.of();
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            permissions = permissionRepository.findByIdIn(request.getPermissionIds());
        }

        Role role = Role.builder()
                .name(request.getName().toUpperCase())
                .description(request.getDescription())
                .isSystemRole(false)
                .permissions(permissions)
                .build();

        Role saved = roleRepository.save(role);
        log.info("Rol creado: {}", saved.getName());
        return userMapper.toDTO(saved);
    }

    @Transactional
    public RoleDTO updateRole(Long id, UpdateRoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + id));

        if (Boolean.TRUE.equals(role.getIsSystemRole()) && request.getPermissionIds() == null) {
            throw new IllegalArgumentException("No se puede modificar un rol del sistema");
        }

        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }

        if (request.getPermissionIds() != null) {
            Set<Permission> permissions = permissionRepository.findByIdIn(request.getPermissionIds());
            role.setPermissions(permissions);
        }

        return userMapper.toDTO(roleRepository.save(role));
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + id));

        if (Boolean.TRUE.equals(role.getIsSystemRole())) {
            throw new IllegalArgumentException("No se pueden eliminar roles del sistema");
        }

        roleRepository.delete(role);
        log.info("Rol eliminado: {}", role.getName());
    }

    @Transactional(readOnly = true)
    public List<PermissionDTO> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PermissionDTO> getPermissionsByModule(String module) {
        return permissionRepository.findByModule(module.toUpperCase()).stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }
}
