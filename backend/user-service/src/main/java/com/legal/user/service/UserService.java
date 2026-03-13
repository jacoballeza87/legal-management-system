package com.legal.user.service;

import com.legal.user.dto.*;
import com.legal.user.exception.UserNotFoundException;
import com.legal.user.mapper.UserMapper;
import com.legal.user.model.*;
import com.legal.user.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CategoryRepository categoryRepository;
    private final UserMapper userMapper;

    @Value("${pagination.default-page-size:20}")
    private int defaultPageSize;

    // ─── Consultas ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<UserDTO> getAllUsers(int page, int size, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);
        Page<UserDTO> result = userRepository.findAll(pageable).map(userMapper::toDTO);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDTO)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con id: " + id));
    }

    @Transactional(readOnly = true)
    public UserDTO getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toDTO)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con email: " + email));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDTO> searchUsers(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<UserDTO> result = userRepository.searchUsers(query, pageable).map(userMapper::toDTO);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDTO> getUsersByStatus(User.UserStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<UserDTO> result = userRepository.findByStatus(status, pageable).map(userMapper::toDTO);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getUsersByRole(String roleName) {
        return userMapper.toDTOList(userRepository.findByRoleName(roleName));
    }

    // ─── Creación ────────────────────────────────────────────────────────────────

    @Transactional
    public UserDTO createUser(CreateUserRequest request, Long createdBy) {
        validateNewUser(request.getEmail(), request.getUsername());

        User user = userMapper.toEntity(request);
        user.setCreatedBy(createdBy);

        // Asignar roles
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            Set<Role> roles = roleRepository.findByIdIn(request.getRoleIds());
            user.setRoles(roles);
        } else {
            // Rol por defecto: USER
            roleRepository.findByName("USER").ifPresent(r -> user.setRoles(Set.of(r)));
        }

        // Asignar categorías
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            user.setCategories(categoryRepository.findByIdIn(request.getCategoryIds()));
        }

        User saved = userRepository.save(user);
        log.info("Usuario creado: {} ({})", saved.getUsername(), saved.getEmail());
        return userMapper.toDTO(saved);
    }

    // ─── Actualización ───────────────────────────────────────────────────────────

    @Transactional
    public UserDTO updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado: " + id));

        userMapper.updateEntity(request, user);

        // Actualizar roles si se especifican
        if (request.getRoleIds() != null) {
            Set<Role> roles = roleRepository.findByIdIn(request.getRoleIds());
            user.setRoles(roles);
        }

        // Actualizar categorías si se especifican
        if (request.getCategoryIds() != null) {
            user.setCategories(categoryRepository.findByIdIn(request.getCategoryIds()));
        }

        User saved = userRepository.save(user);
        log.info("Usuario actualizado: {}", saved.getId());
        return userMapper.toDTO(saved);
    }

    @Transactional
    public UserDTO updateStatus(Long id, User.UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado: " + id));
        userRepository.updateStatus(id, status);
        user.setStatus(status);
        log.info("Estado de usuario {} actualizado a: {}", id, status);
        return userMapper.toDTO(user);
    }

    @Transactional
    public UserDTO assignRoles(Long userId, Set<Long> roleIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado: " + userId));

        Set<Role> roles = roleRepository.findByIdIn(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new IllegalArgumentException("Uno o más roles no existen");
        }
        user.setRoles(roles);
        return userMapper.toDTO(userRepository.save(user));
    }

    @Transactional
    public UserDTO addRole(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado: " + userId));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + roleId));
        user.getRoles().add(role);
        return userMapper.toDTO(userRepository.save(user));
    }

    @Transactional
    public UserDTO removeRole(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado: " + userId));
        user.getRoles().removeIf(r -> r.getId().equals(roleId));
        return userMapper.toDTO(userRepository.save(user));
    }

    // ─── Eliminación ─────────────────────────────────────────────────────────────

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("Usuario no encontrado: " + id);
        }
        userRepository.deleteById(id);
        log.info("Usuario eliminado: {}", id);
    }

    // ─── Estadísticas ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Long> getUserStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", userRepository.count());
        stats.put("active", userRepository.countByStatus(User.UserStatus.ACTIVE));
        stats.put("inactive", userRepository.countByStatus(User.UserStatus.INACTIVE));
        stats.put("suspended", userRepository.countByStatus(User.UserStatus.SUSPENDED));
        stats.put("pending", userRepository.countByStatus(User.UserStatus.PENDING_VERIFICATION));
        return stats;
    }

    // ─── Helpers privados ────────────────────────────────────────────────────────

    private void validateNewUser(String email, String username) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("El email ya está registrado: " + email);
        }
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("El username ya está en uso: " + username);
        }
    }
}
