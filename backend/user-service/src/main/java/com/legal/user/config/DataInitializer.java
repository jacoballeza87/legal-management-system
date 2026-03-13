package com.legal.user.config;

import com.legal.user.model.Permission;
import com.legal.user.model.Role;
import com.legal.user.repository.PermissionRepository;
import com.legal.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        initPermissions();
        initRoles();
    }

    private void initPermissions() {
        List<String[]> perms = List.of(
            // module, action, description
            new String[]{"CASES",     "READ",   "Ver casos"},
            new String[]{"CASES",     "WRITE",  "Crear y editar casos"},
            new String[]{"CASES",     "DELETE", "Eliminar casos"},
            new String[]{"CASES",     "ADMIN",  "Administrar todos los casos"},
            new String[]{"USERS",     "READ",   "Ver usuarios"},
            new String[]{"USERS",     "WRITE",  "Crear y editar usuarios"},
            new String[]{"USERS",     "DELETE", "Eliminar usuarios"},
            new String[]{"USERS",     "ADMIN",  "Administrar usuarios"},
            new String[]{"DOCUMENTS", "READ",   "Ver documentos"},
            new String[]{"DOCUMENTS", "WRITE",  "Subir y editar documentos"},
            new String[]{"DOCUMENTS", "DELETE", "Eliminar documentos"},
            new String[]{"REPORTS",   "READ",   "Ver reportes"},
            new String[]{"REPORTS",   "WRITE",  "Generar reportes"},
            new String[]{"BILLING",   "READ",   "Ver facturación"},
            new String[]{"BILLING",   "WRITE",  "Gestionar facturación"},
            new String[]{"SETTINGS",  "ADMIN",  "Configuración del sistema"}
        );

        for (String[] p : perms) {
            String name = p[0].toLowerCase() + ":" + p[1].toLowerCase();
            if (!permissionRepository.existsByName(name)) {
                permissionRepository.save(Permission.builder()
                        .name(name)
                        .module(p[0])
                        .action(p[1])
                        .description(p[2])
                        .build());
            }
        }
        log.info("Permisos inicializados");
    }

    private void initRoles() {
        Map<String, List<String>> roleDefs = new LinkedHashMap<>();
        roleDefs.put("SUPER_ADMIN", permissionRepository.findAll()
                .stream().map(Permission::getName).toList());
        roleDefs.put("ADMIN", List.of(
                "cases:read","cases:write","cases:delete","cases:admin",
                "users:read","users:write",
                "documents:read","documents:write","documents:delete",
                "reports:read","reports:write","billing:read","billing:write"));
        roleDefs.put("LAWYER", List.of(
                "cases:read","cases:write",
                "documents:read","documents:write",
                "reports:read"));
        roleDefs.put("ACCOUNTANT", List.of(
                "cases:read",
                "documents:read","documents:write",
                "reports:read","reports:write",
                "billing:read","billing:write"));
        roleDefs.put("USER", List.of(
                "cases:read",
                "documents:read",
                "reports:read"));
        roleDefs.put("VIEWER", List.of(
                "cases:read",
                "documents:read"));

        for (Map.Entry<String, List<String>> entry : roleDefs.entrySet()) {
            if (!roleRepository.existsByName(entry.getKey())) {
                Set<Permission> perms = new HashSet<>();
                for (String pName : entry.getValue()) {
                    permissionRepository.findByName(pName).ifPresent(perms::add);
                }
                roleRepository.save(Role.builder()
                        .name(entry.getKey())
                        .description("Rol " + entry.getKey())
                        .isSystemRole(true)
                        .permissions(perms)
                        .build());
            }
        }
        log.info("Roles del sistema inicializados");
    }
}
