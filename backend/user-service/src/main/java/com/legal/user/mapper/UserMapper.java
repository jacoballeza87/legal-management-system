package com.legal.user.mapper;

import com.legal.user.dto.*;
import com.legal.user.model.*;
import org.mapstruct.*;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    // User → UserDTO
    UserDTO toDTO(User user);
    List<UserDTO> toDTOList(List<User> users);

    // CreateUserRequest → User
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "isEmailVerified", constant = "false")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastLogin", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    User toEntity(CreateUserRequest request);

    // UpdateUserRequest → User (solo campos no nulos)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastLogin", ignore = true)
    @Mapping(target = "isEmailVerified", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntity(UpdateUserRequest request, @MappingTarget User user);

    // Role ↔ RoleDTO
    RoleDTO toDTO(Role role);
    Set<RoleDTO> toRoleDTOSet(Set<Role> roles);

    // Permission ↔ PermissionDTO
    PermissionDTO toDTO(Permission permission);
    Set<PermissionDTO> toPermissionDTOSet(Set<Permission> permissions);

    // Category ↔ CategoryDTO
    CategoryDTO toDTO(Category category);
    Set<CategoryDTO> toCategoryDTOSet(Set<Category> categories);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "users", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    Category toEntity(CreateCategoryRequest request);
}
