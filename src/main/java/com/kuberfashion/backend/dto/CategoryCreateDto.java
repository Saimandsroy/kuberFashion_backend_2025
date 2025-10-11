package com.kuberfashion.backend.dto;

import jakarta.validation.constraints.*;

/**
 * DTO for Category Creation/Update - Aligned with Zod CategorySchema
 * Provides comprehensive validation matching frontend Zod validation
 */
public class CategoryCreateDto {
    
    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s&-]+$", message = "Category name can only contain letters, numbers, spaces, & and -")
    private String name;
    
    @NotBlank(message = "Category slug is required")
    @Size(max = 100, message = "Category slug must not exceed 100 characters")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    private String slug;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    @Pattern(regexp = "^https?://.+", message = "Please enter a valid URL")
    private String image;
    
    private Boolean active = true;
    
    // Constructors
    public CategoryCreateDto() {}
    
    public CategoryCreateDto(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }
    
    public CategoryCreateDto(String name, String slug, String description, String image) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.image = image;
    }
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    
    // Utility methods
    public String getNormalizedSlug() {
        return slug != null ? slug.toLowerCase().trim() : null;
    }
    
    public String getTrimmedName() {
        return name != null ? name.trim() : null;
    }
    
    public String getTrimmedDescription() {
        return description != null ? description.trim() : null;
    }
    
    // Method to generate slug from name if not provided
    public String generateSlugFromName() {
        if (name == null) return null;
        return name.toLowerCase()
                  .replaceAll("[^a-z0-9\\s-]", "")
                  .replaceAll("\\s+", "-")
                  .replaceAll("-+", "-")
                  .replaceAll("^-|-$", "");
    }
}
