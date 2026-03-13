package com.legal.user.service;

import com.legal.user.dto.*;
import com.legal.user.mapper.UserMapper;
import com.legal.user.model.Category;
import com.legal.user.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategories(Boolean onlyActive) {
        List<Category> categories = (onlyActive != null && onlyActive)
                ? categoryRepository.findByIsActive(true)
                : categoryRepository.findAll();
        return categories.stream().map(userMapper::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryDTO getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .map(userMapper::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada: " + id));
    }

    @Transactional
    public CategoryDTO createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("La categoría ya existe: " + request.getName());
        }
        Category saved = categoryRepository.save(userMapper.toEntity(request));
        log.info("Categoría creada: {}", saved.getName());
        return userMapper.toDTO(saved);
    }

    @Transactional
    public CategoryDTO updateCategory(Long id, CreateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada: " + id));

        if (request.getName() != null) category.setName(request.getName());
        if (request.getDescription() != null) category.setDescription(request.getDescription());
        if (request.getColorHex() != null) category.setColorHex(request.getColorHex());
        if (request.getIcon() != null) category.setIcon(request.getIcon());

        return userMapper.toDTO(categoryRepository.save(category));
    }

    @Transactional
    public void toggleCategory(Long id, boolean active) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada: " + id));
        category.setIsActive(active);
        categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new IllegalArgumentException("Categoría no encontrada: " + id);
        }
        categoryRepository.deleteById(id);
    }
}
