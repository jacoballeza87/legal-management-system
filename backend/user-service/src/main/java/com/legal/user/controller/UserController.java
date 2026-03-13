package com.legal.user.controller;

import com.legal.user.dto.*;
import com.legal.user.model.User;
import com.legal.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Users", description = "Gestión de usuarios")
public class UserController {

    private final UserService userService;

    // ─── GET ─────────────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Listar todos los usuarios (paginado)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<PageResponse<UserDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return ResponseEntity.ok(userService.getAllUsers(page, size, sortBy, direction));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Obtener usuario por email (uso interno / API Gateway)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<UserDTO> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar usuarios por nombre, email o username")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<PageResponse<UserDTO>> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userService.searchUsers(q, page, size));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Filtrar usuarios por estado")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<PageResponse<UserDTO>> getUsersByStatus(
            @PathVariable User.UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userService.getUsersByStatus(status, page, size));
    }

    @GetMapping("/role/{roleName}")
    @Operation(summary = "Obtener usuarios por rol")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<List<UserDTO>> getUsersByRole(@PathVariable String roleName) {
        return ResponseEntity.ok(userService.getUsersByRole(roleName));
    }

    @GetMapping("/stats")
    @Operation(summary = "Estadísticas de usuarios")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(userService.getUserStats());
    }

    // ─── POST / PUT / PATCH ───────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Crear nuevo usuario")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<UserDTO> createUser(
            @Valid @RequestBody CreateUserRequest request,
            HttpServletRequest httpRequest) {
        Long createdBy = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request, createdBy));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar usuario")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cambiar estado del usuario")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<UserDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam User.UserStatus status) {
        return ResponseEntity.ok(userService.updateStatus(id, status));
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "Asignar roles al usuario (reemplaza todos)")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserDTO> assignRoles(
            @PathVariable Long id,
            @RequestBody Set<Long> roleIds) {
        return ResponseEntity.ok(userService.assignRoles(id, roleIds));
    }

    @PostMapping("/{id}/roles/{roleId}")
    @Operation(summary = "Agregar un rol al usuario")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<UserDTO> addRole(
            @PathVariable Long id,
            @PathVariable Long roleId) {
        return ResponseEntity.ok(userService.addRole(id, roleId));
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    @Operation(summary = "Quitar un rol del usuario")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<UserDTO> removeRole(
            @PathVariable Long id,
            @PathVariable Long roleId) {
        return ResponseEntity.ok(userService.removeRole(id, roleId));
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar usuario")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "Usuario eliminado exitosamente"));
    }
}
