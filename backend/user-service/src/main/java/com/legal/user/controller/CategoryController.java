package com.legal.user.controller;

import com.legal.user.dto.*;
import com.legal.user.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Categories", description = "Gestión de categorías de casos")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Listar categorías")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CategoryDTO>> getAllCategories(
            @RequestParam(required = false) Boolean onlyActive) {
        return ResponseEntity.ok(categoryService.getAllCategories(onlyActive));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener categoría por ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PostMapping
    @Operation(summary = "Crear categoría")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar categoría")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<CategoryDTO> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Activar o desactivar categoría")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, String>> toggleCategory(
            @PathVariable Long id,
            @RequestParam boolean active) {
        categoryService.toggleCategory(id, active);
        return ResponseEntity.ok(Map.of("message", "Categoría " + (active ? "activada" : "desactivada")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar categoría")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(Map.of("message", "Categoría eliminada exitosamente"));
    }
}
