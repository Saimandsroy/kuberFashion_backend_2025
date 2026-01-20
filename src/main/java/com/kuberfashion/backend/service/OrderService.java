package com.kuberfashion.backend.service;

import com.kuberfashion.backend.dto.CartItemDto;
import com.kuberfashion.backend.entity.Address;
import com.kuberfashion.backend.entity.Order;
import com.kuberfashion.backend.entity.OrderItem;
import com.kuberfashion.backend.entity.Product;
import com.kuberfashion.backend.entity.User;
import com.kuberfashion.backend.exception.ResourceNotFoundException;
import com.kuberfashion.backend.repository.AddressRepository;
import com.kuberfashion.backend.repository.OrderRepository;
import com.kuberfashion.backend.repository.ProductRepository;
import com.kuberfashion.backend.repository.ReferralRelationRepository;
import com.kuberfashion.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ReferralService referralService;

    @Autowired
    private ReferralRelationRepository referralRelationRepository;

    // KVision category slug (updated to "kvision")
    private static final String KVISION_CATEGORY_SLUG = "kvision";

    public Order createOrder(Long userId, List<CartItemDto> cartItems, Long addressId,
            Order.PaymentMethod paymentMethod, String referralCode, Order.PurchaseMode purchaseMode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Fetch the shipping address
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

        // Check if cart contains KVision (Footwear) products
        boolean hasKvisionProduct = false;
        for (CartItemDto cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with id: " + cartItem.getProductId()));
            if (product.getCategory() != null &&
                    KVISION_CATEGORY_SLUG.equalsIgnoreCase(product.getCategory().getSlug())) {
                hasKvisionProduct = true;
                break;
            }
        }

        // KVision membership logic
        if (hasKvisionProduct) {
            // Check if user is already a member (has a parent in referral tree)
            boolean isMember = referralRelationRepository.findByUserId(userId).isPresent();

            if (isMember) {
                // Member can buy KVision products again without referral code
                // No referral processing needed - they're already in the network
                logger.info("✅ KVision member {} purchasing additional KVision products", user.getPhone());
            } else {
                // Non-member must provide referral code (Genesis or Member code)
                if (referralCode == null || referralCode.trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Referral code is required to purchase KVision products. Please enter a valid referral code to join KVision.");
                }

                // Only validate referral code (don't process yet - will be done after payment)
                referralService.validateReferralCodeForUser(referralCode, user);
                // Store the referral code on the order for processing after payment
            }
        }

        Order order = new Order();
        order.setUser(user);
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentMethod(paymentMethod);
        order.setReferralCode(referralCode); // Store for processing after payment
        order.setPurchaseMode(purchaseMode != null ? purchaseMode : Order.PurchaseMode.ONLINE);

        // Set payment status based on payment method
        if (paymentMethod == Order.PaymentMethod.CASH_ON_DELIVERY) {
            order.setPaymentStatus(Order.PaymentStatus.PENDING);
        } else {
            order.setPaymentStatus(Order.PaymentStatus.PENDING);
        }

        // Set shipping address fields from Address entity
        order.setShippingFirstName(address.getFirstName());
        order.setShippingLastName(address.getLastName());
        order.setShippingAddressLine1(address.getAddressLine1());
        order.setShippingAddressLine2(address.getAddressLine2());
        order.setShippingCity(address.getCity());
        order.setShippingState(address.getState());
        order.setShippingPostalCode(address.getPostalCode());
        order.setShippingCountry(address.getCountry());
        order.setShippingPhone(address.getPhone());
        order.setShippingAddress(address.getFullAddress());
        order.setBillingAddress(address.getFullAddress());

        // Initialize orderItems set
        order.setOrderItems(new HashSet<>());

        BigDecimal subtotal = BigDecimal.ZERO;
        int totalItems = 0;

        for (CartItemDto cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with id: " + cartItem.getProductId()));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setSelectedSize(cartItem.getSelectedSize());
            orderItem.setSelectedColor(cartItem.getSelectedColor());

            BigDecimal itemSubtotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(itemSubtotal);
            totalItems += cartItem.getQuantity();

            order.getOrderItems().add(orderItem);
        }

        // Calculate shipping (tiered pricing) - OFFLINE purchases have zero shipping
        BigDecimal shippingCost;
        if (purchaseMode == Order.PurchaseMode.OFFLINE) {
            shippingCost = BigDecimal.ZERO; // No shipping for shop pickup
        } else if (subtotal.compareTo(BigDecimal.valueOf(999)) > 0) {
            shippingCost = BigDecimal.valueOf(49); // Orders > ₹999
        } else if (subtotal.compareTo(BigDecimal.valueOf(500)) >= 0) {
            shippingCost = BigDecimal.valueOf(59); // Orders ₹500 - ₹999
        } else {
            shippingCost = BigDecimal.valueOf(79); // Orders < ₹500
        }

        // No tax (GST removed as per user request)
        BigDecimal tax = BigDecimal.ZERO;

        order.setSubtotal(subtotal);
        order.setShippingAmount(shippingCost);
        order.setTaxAmount(tax);
        order.setTotalAmount(subtotal.add(shippingCost).add(tax));
        order.setTotalItems(totalItems);

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
