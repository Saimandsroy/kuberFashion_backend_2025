package com.kuberfashion.backend.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for Product Creation/Update - Aligned with Zod ProductSchema
 * Provides comprehensive validation matching frontend Zod validation
 */
public class ProductCreateDto {
    
    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name must not exceed 200 characters")
    private String name;
    
    @NotBlank(message = "Product slug is required")
    @Size(max = 200, message = "Product slug must not exceed 200 characters")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    private String slug;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @DecimalMax(value = "999999.99", message = "Price cannot exceed ₹999,999.99")
    @Digits(integer = 7, fraction = 2, message = "Price must have at most 2 decimal places")
    private BigDecimal price;
    
    @DecimalMin(value = "0.01", message = "Original price must be greater than 0")
    @DecimalMax(value = "999999.99", message = "Original price cannot exceed ₹999,999.99")
    @Digits(integer = 7, fraction = 2, message = "Original price must have at most 2 decimal places")
    private BigDecimal originalPrice;
    
    @Min(value = 0, message = "Discount cannot be negative")
    @Max(value = 100, message = "Discount cannot exceed 100%")
    private Integer discount = 0;
    
    @NotNull(message = "Please select a valid category")
    @Positive(message = "Please select a valid category")
    private Long categoryId;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    @Pattern(regexp = "^https?://.+", message = "Please enter a valid URL")
    private String image;
    
    @Size(max = 10, message = "Maximum 10 images allowed")
    private List<@Pattern(regexp = "^https?://.+", message = "Please enter a valid URL") String> images;
    
    @Size(max = 20, message = "Maximum 20 sizes allowed")
    private List<@NotBlank(message = "Size cannot be empty") String> sizes;
    
    @Size(max = 20, message = "Maximum 20 colors allowed")
    private List<@NotBlank(message = "Color cannot be empty") String> colors;
    
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity = 0;
    
    private Boolean inStock = true;
    
    private Boolean featured = false;
    
    private Boolean active = true;
    
    // Constructors
    public ProductCreateDto() {}
    
    public ProductCreateDto(String name, String slug, BigDecimal price, Long categoryId) {
        this.name = name;
        this.slug = slug;
        this.price = price;
        this.categoryId = categoryId;
    }
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    
    public Integer getDiscount() { return discount; }
    public void setDiscount(Integer discount) { this.discount = discount; }
    
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    
    public List<String> getSizes() { return sizes; }
    public void setSizes(List<String> sizes) { this.sizes = sizes; }
    
    public List<String> getColors() { return colors; }
    public void setColors(List<String> colors) { this.colors = colors; }
    
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    
    public Boolean getInStock() { return inStock; }
    public void setInStock(Boolean inStock) { this.inStock = inStock; }
    
    public Boolean getFeatured() { return featured; }
    public void setFeatured(Boolean featured) { this.featured = featured; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    
    // Custom validation methods
    public boolean isOriginalPriceValid() {
        if (originalPrice == null) return true;
        return originalPrice.compareTo(price) >= 0;
    }
    
    // Method to get normalized slug
    public String getNormalizedSlug() {
        return slug != null ? slug.toLowerCase().trim() : null;
    }
    
    // Method to calculate effective price
    public BigDecimal getEffectivePrice() {
        if (originalPrice != null && discount != null && discount > 0) {
            BigDecimal discountAmount = originalPrice.multiply(BigDecimal.valueOf(discount)).divide(BigDecimal.valueOf(100));
            return originalPrice.subtract(discountAmount);
        }
        return price;
    }
}
