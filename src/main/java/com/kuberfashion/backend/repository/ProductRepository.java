package com.kuberfashion.backend.repository;

import com.kuberfashion.backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // Fetch join to avoid lazy loading issues
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.slug = :slug")
    Optional<Product> findBySlug(@Param("slug") String slug);
    
    // Fetch join for collections to prevent LazyInitializationException
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.active = true")
    List<Product> findByActiveTrue();
    
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.featured = true AND p.active = true")
    List<Product> findByFeaturedTrueAndActiveTrue();
    
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.category c " +
           "WHERE c.slug = :categorySlug AND p.active = true")
    List<Product> findByCategorySlugAndActiveTrue(@Param("categorySlug") String categorySlug);
    
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.category c " +
           "WHERE c.slug = :categorySlug AND p.active = true")
    Page<Product> findByCategorySlugAndActiveTrue(@Param("categorySlug") String categorySlug, Pageable pageable);
    
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.active = true AND p.inStock = true")
    List<Product> findAvailableProducts();
    
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchProducts(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.category c " +
           "WHERE p.active = true AND c.slug = :categorySlug AND " +
           "p.price BETWEEN :minPrice AND :maxPrice")
    Page<Product> findByCategoryAndPriceRange(@Param("categorySlug") String categorySlug,
                                            @Param("minPrice") BigDecimal minPrice,
                                            @Param("maxPrice") BigDecimal maxPrice,
                                            Pageable pageable);
    
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.active = true AND " +
           "p.price BETWEEN :minPrice AND :maxPrice")
    Page<Product> findByPriceRange(@Param("minPrice") BigDecimal minPrice,
                                 @Param("maxPrice") BigDecimal maxPrice,
                                 Pageable pageable);
    
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.active = true ORDER BY p.rating DESC")
    List<Product> findTopRatedProducts(Pageable pageable);
    
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.active = true ORDER BY p.createdAt DESC")
    List<Product> findNewestProducts(Pageable pageable);
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true")
    long countActiveProducts();
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true AND p.inStock = true")
    long countAvailableProducts();
    
    boolean existsBySlug(String slug);
}
