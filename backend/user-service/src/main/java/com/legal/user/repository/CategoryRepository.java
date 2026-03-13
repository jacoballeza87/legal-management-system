package com.legal.user.repository;

import com.legal.user.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByName(String name);
    List<Category> findByIsActive(Boolean isActive);
    Set<Category> findByIdIn(Set<Long> ids);
}
