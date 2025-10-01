package com.kuberfashion.backend.service;

import com.kuberfashion.backend.dto.CartItemDto;
import com.kuberfashion.backend.entity.Order;
import com.kuberfashion.backend.entity.OrderItem;
import com.kuberfashion.backend.entity.Product;
import com.kuberfashion.backend.entity.User;
import com.kuberfashion.backend.exception.ResourceNotFoundException;
import com.kuberfashion.backend.repository.OrderRepository;
import com.kuberfashion.backend.repository.ProductRepository;
import com.kuberfashion.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    public Order createOrder(Long userId, List<CartItemDto> cartItems, String shippingAddress, 
                           String billingAddress, Order.PaymentMethod paymentMethod) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        Order order = new Order();
        order.setUser(user);
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentStatus(Order.PaymentStatus.PENDING);
        order.setShippingAddress(shippingAddress);
        order.setBillingAddress(billingAddress);
        order.setPaymentMethod(paymentMethod);
        
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        
        for (CartItemDto cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + cartItem.getProductId()));
            
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setSelectedSize(cartItem.getSelectedSize());
            orderItem.setSelectedColor(cartItem.getSelectedColor());
            
            BigDecimal itemSubtotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            BigDecimal itemOriginalTotal = product.getOriginalPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            BigDecimal itemDiscount = itemOriginalTotal.subtract(itemSubtotal);
            
            subtotal = subtotal.add(itemSubtotal);
            totalDiscount = totalDiscount.add(itemDiscount);
            
            order.getOrderItems().add(orderItem);
        }
        
        // Calculate shipping (free shipping over $100)
        BigDecimal shippingCost = subtotal.compareTo(BigDecimal.valueOf(100)) >= 0 ? 
                BigDecimal.ZERO : BigDecimal.valueOf(9.99);
        
        // Calculate tax (8% tax rate)
        BigDecimal taxRate = BigDecimal.valueOf(0.08);
        BigDecimal tax = subtotal.multiply(taxRate);
        
        order.setSubtotal(subtotal);
        order.setShippingAmount(shippingCost);
        order.setTaxAmount(tax);
        order.setTotalAmount(subtotal.add(shippingCost).add(tax));
        
        return orderRepository.save(order);
    }
    
    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }
    
    public Optional<Order> getOrderByIdAndUserId(Long orderId, Long userId) {
        return orderRepository.findByIdAndUserId(orderId, userId);
    }
    
    public Page<Order> getUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    
    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatusOrderByCreatedAtDesc(status);
    }
    
    public Order updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        
        if (status == Order.OrderStatus.SHIPPED) {
            order.setShippedAt(LocalDateTime.now());
        } else if (status == Order.OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        }
        
        return orderRepository.save(order);
    }
    
    public Order updatePaymentStatus(Long orderId, Order.PaymentStatus paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        
        order.setPaymentStatus(paymentStatus);
        order.setUpdatedAt(LocalDateTime.now());
        
        if (paymentStatus == Order.PaymentStatus.PAID) {
            order.setPaidAt(LocalDateTime.now());
            // Auto-confirm order when payment is successful
            if (order.getStatus() == Order.OrderStatus.PENDING) {
                order.setStatus(Order.OrderStatus.CONFIRMED);
            }
        }
        
        return orderRepository.save(order);
    }
    
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        
        if (order.getStatus() == Order.OrderStatus.SHIPPED || 
            order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel order that has been shipped or delivered");
        }
        
        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        
        orderRepository.save(order);
    }
    
    public BigDecimal getTotalRevenue() {
        return orderRepository.getTotalRevenue();
    }
    
    public BigDecimal getRevenueByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderRepository.getRevenueByDateRange(startDate, endDate);
    }
    
    public Long getTotalOrdersCount() {
        return orderRepository.count();
    }
    
    public Long getOrdersCountByStatus(Order.OrderStatus status) {
        return orderRepository.countByStatus(status);
    }
    
    private String generateOrderNumber() {
        // Generate order number with timestamp and random component
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 1000);
        return "KF" + timestamp + String.format("%03d", random);
    }
}
