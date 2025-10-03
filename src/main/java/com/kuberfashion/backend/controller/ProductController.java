package com.kuberfashion.backend.controller;

import com.kuberfashion.backend.dto.ApiResponse;
import com.kuberfashion.backend.dto.ProductResponseDto;
import com.kuberfashion.backend.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:5173")
public class ProductController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    
    @Autowired
    private ProductService productService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getAllProducts() {
        logger.info("📦 GET /api/products - Fetching all products");
        try {
            List<ProductResponseDto> products = productService.getAllProducts();
            logger.info("✅ Successfully retrieved {} products", products.size());
            return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", products));
        } catch (Exception e) {
            logger.error("❌ Error fetching products: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProductById(@PathVariable Long id) {
        ProductResponseDto product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", product));
    }
    
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProductBySlug(@PathVariable String slug) {
        ProductResponseDto product = productService.getProductBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", product));
    }
    
    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getFeaturedProducts() {
        List<ProductResponseDto> products = productService.getFeaturedProducts();
        return ResponseEntity.ok(ApiResponse.success("Featured products retrieved successfully", products));
    }
    
    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getTrendingProducts() {
        List<ProductResponseDto> products = productService.getTrendingProducts();
        return ResponseEntity.ok(ApiResponse.success("Trending products retrieved successfully", products));
    }
    
    @GetMapping("/top-products")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getTopRatedProducts(@RequestParam(defaultValue = "10") int limit) {
        List<ProductResponseDto> products = productService.getTopRatedProducts(limit);
        return ResponseEntity.ok(ApiResponse.success("Top rated products retrieved successfully", products));
    }
    
    @GetMapping("/top-rated")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getTopRated(@RequestParam(defaultValue = "10") int limit) {
        List<ProductResponseDto> products = productService.getTopRatedProducts(limit);
        return ResponseEntity.ok(ApiResponse.success("Top rated products retrieved successfully", products));
    }
    
    @GetMapping("/newest")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getNewestProducts(@RequestParam(defaultValue = "10") int limit) {
        List<ProductResponseDto> products = productService.getNewestProducts(limit);
        return ResponseEntity.ok(ApiResponse.success("Newest products retrieved successfully", products));
    }
    
    @GetMapping("/category/{categorySlug}")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getProductsByCategory(@PathVariable String categorySlug) {
        List<ProductResponseDto> products = productService.getProductsByCategory(categorySlug);
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", products));
    }
    
    @GetMapping("/category/{categorySlug}/paginated")
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getProductsByCategoryPaginated(
            @PathVariable String categorySlug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "featured") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Page<ProductResponseDto> products = productService.getProductsByCategoryPaginated(categorySlug, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", products));
    }
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        
        Page<ProductResponseDto> products = productService.searchProducts(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully", products));
    }
    
    @GetMapping("/filter/price")
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getProductsByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        
        Page<ProductResponseDto> products = productService.getProductsByPriceRange(minPrice, maxPrice, page, size);
        return ResponseEntity.ok(ApiResponse.success("Filtered products retrieved successfully", products));
    }
    
    @GetMapping("/filter/category-price")
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getProductsByCategoryAndPriceRange(
            @RequestParam String categorySlug,
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        
        Page<ProductResponseDto> products = productService.getProductsByCategoryAndPriceRange(categorySlug, minPrice, maxPrice, page, size);
        return ResponseEntity.ok(ApiResponse.success("Filtered products retrieved successfully", products));
    }
    
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getAvailableProducts() {
        List<ProductResponseDto> products = productService.getAvailableProducts();
        return ResponseEntity.ok(ApiResponse.success("Available products retrieved successfully", products));
    }
    
    @GetMapping("/stats/total")
    public ResponseEntity<ApiResponse<Long>> getTotalProducts() {
        long total = productService.getTotalProducts();
        return ResponseEntity.ok(ApiResponse.success("Total products count retrieved", total));
    }
    
    @GetMapping("/stats/available")
    public ResponseEntity<ApiResponse<Long>> getAvailableProductsCount() {
        long count = productService.getAvailableProductsCount();
        return ResponseEntity.ok(ApiResponse.success("Available products count retrieved", count));
    }
}
