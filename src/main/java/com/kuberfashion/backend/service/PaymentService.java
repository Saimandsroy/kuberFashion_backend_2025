package com.kuberfashion.backend.service;

import com.kuberfashion.backend.config.PhonePeConfig;
import com.kuberfashion.backend.entity.Order;
import com.kuberfashion.backend.entity.PaymentTransaction;
import com.kuberfashion.backend.exception.ResourceNotFoundException;
import com.kuberfashion.backend.repository.OrderRepository;
import com.kuberfashion.backend.repository.PaymentTransactionRepository;

import com.phonepe.sdk.pg.Env;
import com.phonepe.sdk.pg.payments.v2.StandardCheckoutClient;
import com.phonepe.sdk.pg.payments.v2.models.request.StandardCheckoutPayRequest;
import com.phonepe.sdk.pg.payments.v2.models.response.StandardCheckoutPayResponse;
import com.phonepe.sdk.pg.common.models.response.OrderStatusResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Payment Service for PhonePe Integration using official SDK
 */
@Service
@Transactional
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private PhonePeConfig phonePeConfig;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentTransactionRepository transactionRepository;

    @Autowired
    private ReferralService referralService;

    private StandardCheckoutClient phonePeClient;

    @PostConstruct
    public void init() {
        if (phonePeConfig.isConfigured()) {
            try {
                Env env = phonePeConfig.isSandbox() ? Env.SANDBOX : Env.PRODUCTION;
                phonePeClient = StandardCheckoutClient.getInstance(
                        phonePeConfig.getClientId(),
                        phonePeConfig.getClientSecret(),
                        Integer.parseInt(phonePeConfig.getClientVersion()),
                        env);
                logger.info("✅ PhonePe SDK initialized successfully in {} mode", env);
            } catch (Exception e) {
                logger.error("❌ Failed to initialize PhonePe SDK: {}", e.getMessage());
            }
        } else {
            logger.warn("⚠️ PhonePe SDK not configured - payment features will be limited");
        }
    }

    /**
     * Initiate a PhonePe payment for an order
     */
    public Map<String, String> initiatePayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Generate unique merchant order ID
        String merchantOrderId = "KF" + System.currentTimeMillis()
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Amount in paise (multiply by 100)
        long amountInPaise = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue();

        // Create payment transaction record
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrder(order);
        transaction.setMerchantOrderId(merchantOrderId);
        transaction.setAmount(order.getTotalAmount());
        transaction.setStatus(PaymentTransaction.TransactionStatus.INITIATED);

        String redirectUrl;

        if (phonePeClient != null) {
            try {
                // Build the redirect URL with merchantOrderId
                String callbackRedirectUrl = phonePeConfig.getRedirectUrl() + "?merchantOrderId=" + merchantOrderId;

                // Create PhonePe payment request
                StandardCheckoutPayRequest payRequest = StandardCheckoutPayRequest.builder()
                        .merchantOrderId(merchantOrderId)
                        .amount(amountInPaise)
                        .redirectUrl(callbackRedirectUrl)
                        .build();

                // Initiate payment with PhonePe
                StandardCheckoutPayResponse payResponse = phonePeClient.pay(payRequest);
                redirectUrl = payResponse.getRedirectUrl();

                transaction.setRedirectUrl(redirectUrl);
                transaction.setStatus(PaymentTransaction.TransactionStatus.PENDING);

                logger.info("✅ PhonePe payment initiated for order {} - merchantOrderId: {}",
                        order.getOrderNumber(), merchantOrderId);

            } catch (Exception e) {
                logger.error("❌ PhonePe payment initiation failed: {}", e.getMessage());
                logger.error("❌ Full exception details: ", e);

                // GRACEFUL FALLBACK: Instead of failing, use simulation mode
                logger.warn("⚠️ Falling back to simulated payment mode due to SDK error");
                redirectUrl = phonePeConfig.getRedirectUrl() + "?merchantOrderId=" + merchantOrderId
                        + "&simulated=true&error=sdk_failure";
                transaction.setRedirectUrl(redirectUrl);
                transaction.setStatus(PaymentTransaction.TransactionStatus.PENDING);
                transaction.setErrorMessage("PhonePe SDK failed, using simulation: "
                        + (e.getMessage() != null ? e.getMessage() : e.getClass().getName()));
            }
        } else {
            // Fallback for when SDK is not configured - simulation mode
            redirectUrl = phonePeConfig.getRedirectUrl() + "?merchantOrderId=" + merchantOrderId + "&simulated=true";
            transaction.setRedirectUrl(redirectUrl);
            transaction.setStatus(PaymentTransaction.TransactionStatus.PENDING);
            logger.warn("⚠️ PhonePe SDK not available - using simulated payment URL");
        }

        transactionRepository.save(transaction);

        // Update order payment status
        order.setPaymentStatus(Order.PaymentStatus.PENDING);
        order.setPaymentMethod(Order.PaymentMethod.PHONEPE);
        orderRepository.save(order);

        Map<String, String> result = new HashMap<>();
        result.put("merchantOrderId", merchantOrderId);
        result.put("redirectUrl", redirectUrl);
        result.put("orderId", order.getId().toString());

        return result;
    }

    /**
     * Check payment status from PhonePe
     */
    public Map<String, Object> checkPaymentStatus(String merchantOrderId) {
        PaymentTransaction transaction = transactionRepository.findByMerchantOrderId(merchantOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + merchantOrderId));

        Map<String, Object> result = new HashMap<>();
        result.put("merchantOrderId", merchantOrderId);
        result.put("orderId", transaction.getOrderId());
        result.put("amount", transaction.getAmount());
        result.put("status", transaction.getStatus().name());

        if (phonePeClient != null && transaction.getStatus() == PaymentTransaction.TransactionStatus.PENDING) {
            try {
                // Check status with PhonePe SDK
                OrderStatusResponse statusResponse = phonePeClient.getOrderStatus(merchantOrderId);
                String state = statusResponse.getState();

                logger.info("PhonePe order status for {}: {}", merchantOrderId, state);

                if ("COMPLETED".equalsIgnoreCase(state)) {
                    markPaymentSuccess(merchantOrderId, statusResponse.getOrderId());
                    result.put("status", "COMPLETED");
                } else if ("FAILED".equalsIgnoreCase(state)) {
                    markPaymentFailed(merchantOrderId, "PAYMENT_FAILED", "Payment failed at PhonePe");
                    result.put("status", "FAILED");
                } else {
                    result.put("status", state);
                }
            } catch (Exception e) {
                logger.error("Error checking PhonePe payment status: {}", e.getMessage());
                result.put("error", e.getMessage());
            }
        }

        return result;
    }

    /**
     * Handle PhonePe webhook/callback
     */
    public boolean handleCallback(String authorization, String requestBody) {
        logger.info("Received PhonePe callback: {}", requestBody);

        // For now, just log and acknowledge
        // The actual callback validation should use PhonePe SDK methods
        // when they are documented

        return true;
    }

    /**
     * Mark payment as successful
     */
    public void markPaymentSuccess(String merchantOrderId, String transactionId) {
        PaymentTransaction transaction = transactionRepository.findByMerchantOrderId(merchantOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + merchantOrderId));

        transaction.setStatus(PaymentTransaction.TransactionStatus.COMPLETED);
        transaction.setPhonePeTransactionId(transactionId);
        transactionRepository.save(transaction);

        // Update order
        Order order = transaction.getOrder();
        order.setPaymentStatus(Order.PaymentStatus.PAID);
        order.setPaymentTransactionId(transactionId);
        order.setPaidAt(LocalDateTime.now());
        order.setStatus(Order.OrderStatus.CONFIRMED);
        orderRepository.save(order);

        // Process referral ONLY after successful payment
        String referralCode = order.getReferralCode();
        if (referralCode != null && !referralCode.trim().isEmpty()) {
            try {
                referralService.handlePostRegistration(order.getUser(), referralCode);
                logger.info("✅ Referral processed for user {} with code {}",
                        order.getUser().getPhone(), referralCode);
            } catch (Exception e) {
                logger.error("❌ Failed to process referral for order {}: {}",
                        order.getOrderNumber(), e.getMessage());
                // Don't fail the payment confirmation - referral is secondary
            }
        }

        logger.info("✅ Payment marked successful for order: {}", order.getOrderNumber());
    }

    /**
     * Mark payment as failed
     */
    public void markPaymentFailed(String merchantOrderId, String errorCode, String errorMessage) {
        PaymentTransaction transaction = transactionRepository.findByMerchantOrderId(merchantOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + merchantOrderId));

        transaction.setStatus(PaymentTransaction.TransactionStatus.FAILED);
        transaction.setErrorCode(errorCode);
        transaction.setErrorMessage(errorMessage);
        transactionRepository.save(transaction);

        // Update order
        Order order = transaction.getOrder();
        order.setPaymentStatus(Order.PaymentStatus.FAILED);
        orderRepository.save(order);

        logger.info("❌ Payment marked failed for order: {}", order.getOrderNumber());
    }
}
