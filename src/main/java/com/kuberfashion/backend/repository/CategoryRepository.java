package com.kuberfashion.backend.repository;

import com.kuberfashion.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    Optional<Category> findBySlug(String slug);
    
    List<Category> findByActiveTrue();
    
    boolean existsBySlug(String slug);
    
    boolean existsByName(String name);
    
    @Query("SELECT c FROM Category c WHERE c.active = true ORDER BY c.name ASC")
    List<Category> findAllActiveOrderByName();
    
    @Query("SELECT c FROM Category c WHERE c.active = true AND c.productCount > 0 ORDER BY c.productCount DESC")
    List<Category> findActiveCategoriesWithProducts();
}
