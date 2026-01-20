package com.kuberfashion.backend.service;

import com.kuberfashion.backend.dto.CategoryResponseDto;
import com.kuberfashion.backend.entity.Category;
import com.kuberfashion.backend.exception.ResourceNotFoundException;
import com.kuberfashion.backend.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CategoryService {
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    public List<CategoryResponseDto> getAllCategories() {
        return categoryRepository.findAllActiveOrderByName()
                .stream()
                .map(CategoryResponseDto::new)
                .collect(Collectors.toList());
    }
    
    public CategoryResponseDto getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return new CategoryResponseDto(category);
    }
    
    public CategoryResponseDto getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));
        return new CategoryResponseDto(category);
    }
    
    public List<CategoryResponseDto> getCategoriesWithProducts() {
        return categoryRepository.findActiveCategoriesWithProducts()
                .stream()
                .map(CategoryResponseDto::new)
                .collect(Collectors.toList());
    }
    
    public boolean existsBySlug(String slug) {
        return categoryRepository.existsBySlug(slug);
    }
    
    public boolean existsByName(String name) {
        return categoryRepository.existsByName(name);
    }
    
    public Category findBySlug(String slug) {
        return categoryRepository.findBySlug(slug).orElse(null);
    }
}
