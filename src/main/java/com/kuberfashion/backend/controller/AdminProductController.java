package com.kuberfashion.backend.controller;

import com.kuberfashion.backend.dto.ApiResponse;
import com.kuberfashion.backend.dto.ProductResponseDto;
import com.kuberfashion.backend.entity.Product;
import com.kuberfashion.backend.entity.Category;
import com.kuberfashion.backend.service.ProductService;
import com.kuberfashion.backend.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AdminProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponseDto>> createProduct(@Valid @RequestBody ProductCreateDto productDto) {
        try {
            // Find category
            Category category = categoryService.findBySlug(productDto.getCategorySlug());
            if (category == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Category not found: " + productDto.getCategorySlug()));
            }

            // Create product
            Product product = new Product();
            product.setName(productDto.getName());
            product.setSlug(productDto.getSlug());
            product.setPrice(productDto.getPrice());
            product.setOriginalPrice(productDto.getOriginalPrice());
            product.setDiscount(productDto.getDiscount());
            product.setCategory(category);
            product.setImage(productDto.getImage());
            product.setImages(productDto.getImages());
            product.setDescription(productDto.getDescription());
            product.setSizes(productDto.getSizes());
            product.setColors(productDto.getColors());
            product.setInStock(productDto.isInStock());
            product.setFeatured(productDto.isFeatured());
            product.setStockQuantity(productDto.getStockQuantity());
            product.setActive(productDto.isActive());

            Product savedProduct = productService.createProduct(product);
            ProductResponseDto responseDto = new ProductResponseDto(savedProduct);
            
            return new ResponseEntity<>(ApiResponse.success("Product created successfully", responseDto), HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to create product: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponseDto>> updateProduct(
            @PathVariable Long id, 
            @Valid @RequestBody ProductCreateDto productDto) {
        try {
            Product existingProduct = productService.findById(id);
            if (existingProduct == null) {
                return ResponseEntity.notFound().build();
            }

            // Find category if changed
            if (!existingProduct.getCategory().getSlug().equals(productDto.getCategorySlug())) {
                Category category = categoryService.findBySlug(productDto.getCategorySlug());
                if (category == null) {
                    return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Category not found: " + productDto.getCategorySlug()));
                }
                existingProduct.setCategory(category);
            }

            // Update product fields
            existingProduct.setName(productDto.getName());
            existingProduct.setSlug(productDto.getSlug());
            existingProduct.setPrice(productDto.getPrice());
            existingProduct.setOriginalPrice(productDto.getOriginalPrice());
            existingProduct.setDiscount(productDto.getDiscount());
            existingProduct.setImage(productDto.getImage());
            existingProduct.setImages(productDto.getImages());
            existingProduct.setDescription(productDto.getDescription());
            existingProduct.setSizes(productDto.getSizes());
            existingProduct.setColors(productDto.getColors());
            existingProduct.setInStock(productDto.isInStock());
            existingProduct.setFeatured(productDto.isFeatured());
            existingProduct.setStockQuantity(productDto.getStockQuantity());
            existingProduct.setActive(productDto.isActive());

            Product updatedProduct = productService.updateProduct(existingProduct);
            ProductResponseDto responseDto = new ProductResponseDto(updatedProduct);
            
            return ResponseEntity.ok(ApiResponse.success("Product updated successfully", responseDto));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to update product: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteProduct(@PathVariable Long id) {
        try {
            Product product = productService.findById(id);
            if (product == null) {
                return ResponseEntity.notFound().build();
            }

            productService.deleteProduct(id);
            return ResponseEntity.ok(ApiResponse.success("Product deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to delete product: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> updateProductStatus(
            @PathVariable Long id, 
            @RequestBody Map<String, Boolean> statusUpdate) {
        try {
            Product product = productService.findById(id);
            if (product == null) {
                return ResponseEntity.notFound().build();
            }

            if (statusUpdate.containsKey("active")) {
                product.setActive(statusUpdate.get("active"));
            }
            if (statusUpdate.containsKey("featured")) {
                product.setFeatured(statusUpdate.get("featured"));
            }
            if (statusUpdate.containsKey("inStock")) {
                product.setInStock(statusUpdate.get("inStock"));
            }

            productService.updateProduct(product);
            return ResponseEntity.ok(ApiResponse.success("Product status updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to update product status: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> updateStock(
            @PathVariable Long id, 
            @RequestBody Map<String, Integer> stockUpdate) {
        try {
            Product product = productService.findById(id);
            if (product == null) {
                return ResponseEntity.notFound().build();
            }

            Integer newStock = stockUpdate.get("stockQuantity");
            if (newStock != null) {
                product.setStockQuantity(newStock);
                product.setInStock(newStock > 0);
                productService.updateProduct(product);
            }

            return ResponseEntity.ok(ApiResponse.success("Stock updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to update stock: " + e.getMessage()));
        }
    }

    // DTO for product creation/update
    public static class ProductCreateDto {
        private String name;
        private String slug;
        private BigDecimal price;
        private BigDecimal originalPrice;
        private Integer discount = 0;
        private String categorySlug;
        private String image;
        private List<String> images;
        private String description;
        private List<String> sizes;
        private List<String> colors;
        private boolean inStock = true;
        private boolean featured = false;
        private Integer stockQuantity = 0;
        private boolean active = true;

        // Getters and setters
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

        public String getCategorySlug() { return categorySlug; }
        public void setCategorySlug(String categorySlug) { this.categorySlug = categorySlug; }

        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }

        public List<String> getImages() { return images; }
        public void setImages(List<String> images) { this.images = images; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public List<String> getSizes() { return sizes; }
        public void setSizes(List<String> sizes) { this.sizes = sizes; }

        public List<String> getColors() { return colors; }
        public void setColors(List<String> colors) { this.colors = colors; }

        public boolean isInStock() { return inStock; }
        public void setInStock(boolean inStock) { this.inStock = inStock; }

        public boolean isFeatured() { return featured; }
        public void setFeatured(boolean featured) { this.featured = featured; }

        public Integer getStockQuantity() { return stockQuantity; }
        public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }
}
