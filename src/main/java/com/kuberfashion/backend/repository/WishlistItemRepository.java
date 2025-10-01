package com.kuberfashion.backend.repository;

import com.kuberfashion.backend.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    
    @Query("SELECT w FROM WishlistItem w WHERE w.user.id = :userId ORDER BY w.createdAt DESC")
    List<WishlistItem> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT w FROM WishlistItem w WHERE w.user.id = :userId AND w.product.id = :productId")
    Optional<WishlistItem> findByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    @Query("SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END FROM WishlistItem w WHERE w.user.id = :userId AND w.product.id = :productId")
    boolean existsByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
    
    @Query("DELETE FROM WishlistItem w WHERE w.user.id = :userId AND w.product.id = :productId")
    void deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
    
    @Query("DELETE FROM WishlistItem w WHERE w.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(w) FROM WishlistItem w WHERE w.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
    
    @Query("SELECT w FROM WishlistItem w JOIN FETCH w.product WHERE w.user.id = :userId ORDER BY w.createdAt DESC")
    List<WishlistItem> findByUserIdWithProduct(@Param("userId") Long userId);
}
