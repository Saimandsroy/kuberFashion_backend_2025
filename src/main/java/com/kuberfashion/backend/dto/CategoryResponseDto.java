package com.kuberfashion.backend.dto;

import com.kuberfashion.backend.entity.Category;

import java.time.LocalDateTime;

public class CategoryResponseDto {
    
    private Long id;
    private String name;
    private String slug;
    private String image;
    private String description;
    private Integer productCount;
    private boolean active;
    private LocalDateTime createdAt;
    
    // Constructors
    public CategoryResponseDto() {}
    
    public CategoryResponseDto(Category category) {
        this.id = category.getId();
        this.name = category.getName();
        this.slug = category.getSlug();
        this.image = category.getImage();
        this.description = category.getDescription();
        this.productCount = category.getProductCount();
        this.active = category.isActive();
        this.createdAt = category.getCreatedAt();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Integer getProductCount() { return productCount; }
    public void setProductCount(Integer productCount) { this.productCount = productCount; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
