package com.kuberfashion.backend.service;

import com.kuberfashion.backend.dto.CartItemDto;
import com.kuberfashion.backend.entity.CartItem;
import com.kuberfashion.backend.entity.Product;
import com.kuberfashion.backend.entity.User;
import com.kuberfashion.backend.exception.ResourceNotFoundException;
import com.kuberfashion.backend.repository.CartItemRepository;
import com.kuberfashion.backend.repository.ProductRepository;
import com.kuberfashion.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CartService {
    
    @Autowired
    private CartItemRepository cartItemRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public List<CartItemDto> getCartItems(Long userId) {
        // Verify user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return cartItems.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public CartItemDto addToCart(Long userId, Long productId, Integer quantity, String size, String color) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        
        // Check if item already exists in cart
        Optional<CartItem> existingItem = cartItemRepository.findByUserIdAndProductIdAndSizeAndColor(
                userId, productId, size, color);
        
        CartItem cartItem;
        if (existingItem.isPresent()) {
            cartItem = existingItem.get();
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
        } else {
            cartItem = new CartItem();
            cartItem.setUser(user);
            cartItem.setProduct(product);
            cartItem.setQuantity(quantity);
            cartItem.setSelectedSize(size);
            cartItem.setSelectedColor(color);
        }
        
        cartItem = cartItemRepository.save(cartItem);
        return convertToDto(cartItem);
    }
    
    public CartItemDto updateCartItem(Long userId, Long itemId, Integer quantity) {
        CartItem cartItem = cartItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        
        cartItem.setQuantity(quantity);
        cartItem = cartItemRepository.save(cartItem);
        return convertToDto(cartItem);
    }
    
    public void removeFromCart(Long userId, Long itemId) {
        CartItem cartItem = cartItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        
        cartItemRepository.delete(cartItem);
    }
    
    public void clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }
    
    public long getCartCount(Long userId) {
        return cartItemRepository.countByUserId(userId);
    }
    
    private CartItemDto convertToDto(CartItem cartItem) {
        CartItemDto dto = new CartItemDto();
        dto.setId(cartItem.getId());
        dto.setProductId(cartItem.getProduct().getId());
        dto.setQuantity(cartItem.getQuantity());
        dto.setSelectedSize(cartItem.getSelectedSize());
        dto.setSelectedColor(cartItem.getSelectedColor());
        dto.setName(cartItem.getProduct().getName());
        dto.setImage(cartItem.getProduct().getImage());
        dto.setPrice(cartItem.getProduct().getPrice());
        dto.setCategory(cartItem.getProduct().getCategory().getName());
        return dto;
    }
}
