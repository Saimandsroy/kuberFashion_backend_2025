package com.kuberfashion.backend.service;

import com.kuberfashion.backend.dto.ProductResponseDto;
import com.kuberfashion.backend.entity.Product;
import com.kuberfashion.backend.entity.User;
import com.kuberfashion.backend.entity.WishlistItem;
import com.kuberfashion.backend.exception.ResourceNotFoundException;
import com.kuberfashion.backend.repository.ProductRepository;
import com.kuberfashion.backend.repository.UserRepository;
import com.kuberfashion.backend.repository.WishlistItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class WishlistService {
    
    @Autowired
    private WishlistItemRepository wishlistItemRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    public List<ProductResponseDto> getUserWishlist(Long userId) {
        return wishlistItemRepository.findByUserIdWithProduct(userId)
                .stream()
                .map(wishlistItem -> new ProductResponseDto(wishlistItem.getProduct()))
                .collect(Collectors.toList());
    }
    
    public ProductResponseDto addToWishlist(Long userId, Long productId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        
        // Check if item already exists in wishlist
        if (wishlistItemRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new IllegalArgumentException("Product is already in wishlist");
        }
        
        WishlistItem wishlistItem = new WishlistItem(user, product);
        wishlistItemRepository.save(wishlistItem);
        
        return new ProductResponseDto(product);
    }
    
    public void removeFromWishlist(Long userId, Long productId) {
        if (!wishlistItemRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new ResourceNotFoundException("Product not found in wishlist");
        }
        
        wishlistItemRepository.deleteByUserIdAndProductId(userId, productId);
    }
    
    public void clearWishlist(Long userId) {
        wishlistItemRepository.deleteByUserId(userId);
    }
    
    public boolean isInWishlist(Long userId, Long productId) {
        return wishlistItemRepository.existsByUserIdAndProductId(userId, productId);
    }
    
    public long getWishlistCount(Long userId) {
        return wishlistItemRepository.countByUserId(userId);
    }
}
