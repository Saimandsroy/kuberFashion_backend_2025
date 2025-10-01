package com.kuberfashion.backend.service;

import com.kuberfashion.backend.dto.ProductResponseDto;
import com.kuberfashion.backend.entity.Product;
import com.kuberfashion.backend.exception.ResourceNotFoundException;
import com.kuberfashion.backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    public List<ProductResponseDto> getAllProducts() {
        try {
            List<Product> products = productRepository.findByActiveTrue();
            System.out.println("Found " + products.size() + " active products");
            return products.stream()
                    .map(ProductResponseDto::new)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error fetching all products: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return new ProductResponseDto(product);
    }
    
    public ProductResponseDto getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with slug: " + slug));
        return new ProductResponseDto(product);
    }
    
    public List<ProductResponseDto> getFeaturedProducts() {
        return productRepository.findByFeaturedTrueAndActiveTrue()
                .stream()
                .map(ProductResponseDto::new)
                .collect(Collectors.toList());
    }
    
    public List<ProductResponseDto> getProductsByCategory(String categorySlug) {
        return productRepository.findByCategorySlugAndActiveTrue(categorySlug)
                .stream()
                .map(ProductResponseDto::new)
                .collect(Collectors.toList());
    }
    
    public Page<ProductResponseDto> getProductsByCategoryPaginated(String categorySlug, int page, int size, String sortBy, String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, getSortField(sortBy)));
        
        Page<Product> products = productRepository.findByCategorySlugAndActiveTrue(categorySlug, pageable);
        return products.map(ProductResponseDto::new);
    }
    
    public Page<ProductResponseDto> searchProducts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productRepository.searchProducts(keyword, pageable);
        return products.map(ProductResponseDto::new);
    }
    
    public Page<ProductResponseDto> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productRepository.findByPriceRange(minPrice, maxPrice, pageable);
        return products.map(ProductResponseDto::new);
    }
    
    public Page<ProductResponseDto> getProductsByCategoryAndPriceRange(String categorySlug, BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productRepository.findByCategoryAndPriceRange(categorySlug, minPrice, maxPrice, pageable);
        return products.map(ProductResponseDto::new);
    }
    
    public List<ProductResponseDto> getTopRatedProducts(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return productRepository.findTopRatedProducts(pageable)
                .stream()
                .map(ProductResponseDto::new)
                .collect(Collectors.toList());
    }
    
    public List<ProductResponseDto> getNewestProducts(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return productRepository.findNewestProducts(pageable)
                .stream()
                .map(ProductResponseDto::new)
                .collect(Collectors.toList());
    }
    
    public List<ProductResponseDto> getTrendingProducts() {
        // For now, return top rated products as trending
        return getTopRatedProducts(10);
    }
    
    public List<ProductResponseDto> getAvailableProducts() {
        return productRepository.findAvailableProducts()
                .stream()
                .map(ProductResponseDto::new)
                .collect(Collectors.toList());
    }
    
    public long getTotalProducts() {
        return productRepository.countActiveProducts();
    }
    
    public long getAvailableProductsCount() {
        return productRepository.countAvailableProducts();
    }
    
    // Admin methods
    @Transactional
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }
    
    @Transactional
    public Product updateProduct(Product product) {
        return productRepository.save(product);
    }
    
    @Transactional
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
    
    public Product findById(Long id) {
        return productRepository.findById(id).orElse(null);
    }
    
    private String getSortField(String sortBy) {
        switch (sortBy) {
            case "price-low":
            case "price-high":
                return "price";
            case "rating":
                return "rating";
            case "newest":
                return "createdAt";
            case "name":
                return "name";
            default:
                return "id"; // Default sort by id for featured
        }
    }
}
